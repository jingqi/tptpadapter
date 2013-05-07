package parser.record;

/**
 * android trace 二进制头部
 *
 * @author jingqi
 */
public class HeaderRecord {

	// 魔数
	private int magic;
	// 版本
	private int version;
	// 剩余的二进制数据偏移位置
	private int offset;
	// 起始时间
	private long startDateTime; // in usec

	public HeaderRecord(byte[] buf) {
		this(buf, 0);
	}

	public HeaderRecord(byte[] buf, int off) {
		magic = (buf[off] & 0xFF) | ((buf[off + 1] & 0xFF) << 8) |
				((buf[off + 2] & 0xFF) << 16) | ((buf[off + 3] & 0xFF) << 24);
		if (magic != 0x574f4c53)
			throw new RuntimeException();

		version = (buf[off + 4] & 0xFF) | ((buf[off + 5] & 0xFF) << 8);

		offset = (buf[off + 6] & 0xFF) | ((buf[off + 7] & 0xFF) << 8);

		startDateTime = (buf[off + 8] & 0xFFL) | ((buf[off + 9] & 0xFFL) << 8) |
				((buf[off + 10] & 0xFFL) << 16) | ((buf[off + 11] & 0xFFL) << 24) |
				((buf[off + 12] & 0xFFL) << 32) | ((buf[off + 13] & 0xFFL) << 40) |
				((buf[off + 14] & 0xFFL) << 48) | ((buf[off + 15] & 0xFFL) << 56);
	}

	public int getMagic() {
		return magic;
	}

	public int getVersion() {
		return version;
	}

	public int getOffset() {
		return offset;
	}

	public long getStartDateTime() {
		return startDateTime;
	}

	public static int size() {
		return 16;
	}
}
