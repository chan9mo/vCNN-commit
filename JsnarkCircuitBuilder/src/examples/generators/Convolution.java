/*******************************************************************************
 * Author: Ahmed Kosba <akosba@cs.umd.edu>
 *******************************************************************************/
package examples.generators;

import util.Util;

import java.math.BigInteger;

import circuit.config.Config;
import circuit.eval.CircuitEvaluator;
import circuit.eval.Instruction;
import circuit.operations.Gadget;
import circuit.structure.CircuitGenerator;
import circuit.structure.Wire;
import circuit.structure.WireArray;

import examples.gadgets.math.FieldDivisionGadget;

public class Convolution extends CircuitGenerator {

	private Wire[] inputWires;
	private Wire a_k;
	private Wire b_k;
	private Wire test_mul;
	private Wire test_mul_mod;

	private String A;
	private String B;
	private String C;
	
	public Convolution(String circuitName, String A, String B, String C) {
		super(circuitName);
		this.A = A;
		this.B = B;
		this.C = C;
	}

	@Override
	protected void buildCircuit() {
		
		/** declare inputs **/
		inputWires = createInputWireArray(3, "Input Convolution: A(k), B(k), C(k)");

		/** connect gadget **/

		Wire a_k = inputWires[0];
		Wire b_k = inputWires[1];
		Wire test_mul = a_k.mul(b_k);

		test_mul_mod = createProverWitnessWire("modular result");

		specifyProverWitnessComputation(new Instruction() {
			public void evaluate(CircuitEvaluator evaluator) {
				BigInteger aValue = evaluator.getWireValue(test_mul);
				BigInteger bValue = Config.FIELD_PRIME;
				BigInteger rValue = aValue.mod(bValue);
				evaluator.setWireValue(test_mul_mod, rValue);
			}
		});
		
		/** Now compare the multiplication result (A_k * B_k) with the public C_k **/
		Wire diff = test_mul_mod;
		Wire diff2 = diff.sub(inputWires[2]);
		Wire check = diff2.checkNonZero();
		
		/** Expected mismatch here if the sample input below is tried**/
		makeOutput(diff2.checkNonZero(), "0: Proof Success");
		
	}

	@Override
	public void generateSampleInput(CircuitEvaluator circuitEvaluator) {
		//how do i put evaluation value in this?		
		circuitEvaluator.setWireValue(inputWires[0],
				new BigInteger(A));
		circuitEvaluator.setWireValue(inputWires[1],
				new BigInteger(B));
		circuitEvaluator.setWireValue(inputWires[2],
				new BigInteger(C));
	}
	
	public static void main(String[] args) throws Exception {	
		Convolution vcnn = new Convolution("for extraction", "0", "0", "0");
		String inputvalue[] = vcnn.runKateCommitment();

		Convolution vcnn2 = new Convolution("vCNN+: Convolution Proof", inputvalue[0], inputvalue[1], inputvalue[2]);
		vcnn2.generateCircuit();
		vcnn2.evalCircuit();
		vcnn2.prepFiles();
		vcnn2.runLibsnark();		
	}

}
