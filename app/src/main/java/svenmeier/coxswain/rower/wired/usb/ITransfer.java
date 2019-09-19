package svenmeier.coxswain.rower.wired.usb;

import java.util.Iterator;

/**
 */
public interface ITransfer {

	int PARITY_NONE = 0;
	int PARITY_ODD = 1;
	int PARITY_EVEN = 2;
	int PARITY_MARK = 3;
	int PARITY_SPACE = 4;

	int STOP_BIT_1_0 = 0;
	int STOP_BIT_1_5 = 1;
	int STOP_BIT_2_0 = 2;

	void setTimeout(int timeout);

	void setBaudrate(int baudRate);

	void setData(int dataBits, int parity, int stopBits, boolean tx);

	void produce(byte[] b);
	
	Consumer consumer();
}