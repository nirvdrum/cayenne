package org.objectstyle.perform;

/**
 * @author Andrei Adamchik
 */
public class PairResult {
	protected TestResult mainResult;
	protected TestResult refResult;

	/**
	 * Constructor for PairResult.
	 */
	public PairResult(TestResult mainResult, TestResult refResult) {
		super();
		this.mainResult = mainResult;
		this.refResult = refResult;
	}

	public TestResult getMainResult() {
		return mainResult;
	}



	public TestResult getRefResult() {
		return refResult;
	}
}
