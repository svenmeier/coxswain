package svenmeier.coxswain.rower.water.usb;

/**
 */
public interface ITransfer {

	public static final int PARITY_NONE = 0;
	public static final int PARITY_ODD = 1;
	public static final int PARITY_EVEN = 2;
	public static final int PARITY_MARK = 3;
	public static final int PARITY_SPACE = 4;

	void setBaudrate(int baudRate);

	void setData(int dataBits, int parity, int stopBits, boolean tx);

	void setTimeout(int timeout);

	byte[] buffer();

	int bulkInput();

	void bulkOutput(int length);
}
