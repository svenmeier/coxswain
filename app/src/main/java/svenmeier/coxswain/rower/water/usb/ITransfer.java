package svenmeier.coxswain.rower.water.usb;

/**
 */
public interface ITransfer {

	void setBaudRate(int baudRate);

	void setTimeout(int timeout);

	byte[] buffer();

	int bulkInput();

	void bulkOutput(int length);
}
