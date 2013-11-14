package netutils.parse;
/**
 * Holds TCP common constants:<br>
 * TCP flags masks.<br>
 * TCP offsets.<br>
 * 
 * 
 * @author roni bar-yanai
 */
public class TCPPacketType
{
      //	 flag bitmasks

	  final int TCP_URG_MASK = 0x0020;
	  final int TCP_ACK_MASK = 0x0010;
	  final int TCP_PSH_MASK = 0x0008;
	  final int TCP_RST_MASK = 0x0004;
	  final int TCP_SYN_MASK = 0x0002;
	  final int TCP_FIN_MASK = 0x0001;


	  // field lengths

	  /**
	   * Length of a TCP port in bytes.
	   */
	  final int TCP_PORT_LEN = 2;

	  /**
	   * Length of the sequence number in bytes.
	   */
	  final int TCP_SEQ_LEN = 4;

	  /**
	   * Length of the acknowledgment number in bytes.
	   */
	  final int TCP_ACK_LEN = 4;

	  /**
	   * Length of the header length and flags field in bytes.
	   */
	  final int TCP_FLAG_LEN = 2;

	  /**
	   * Length of the window size field in bytes.
	   */
	  final int TCP_WIN_LEN = 2;

	  /**
	   * Length of the checksum field in bytes.
	   */
	  final int TCP_CSUM_LEN = 2;

	  /**
	   * Length of the urgent field in bytes.
	   */
	  final int TCP_URG_LEN = 2;


	  // field positions

	  /**
	   * Position of the source port field.
	   */
	  final int TCP_SP_POS = 0;

	  /**
	   * Position of the destination port field.
	   */
	  final int TCP_DP_POS = TCP_PORT_LEN;

	  /**
	   * Position of the sequence number field.
	   */
	  final int TCP_SEQ_POS = TCP_DP_POS + TCP_PORT_LEN;

	  /**
	   * Position of the acknowledgment number field.
	   */
	  final int TCP_ACK_POS = TCP_SEQ_POS + TCP_SEQ_LEN;

	  /**
	   * Position of the header length and flags field.
	   */
	  final int TCP_FLAG_POS = TCP_ACK_POS + TCP_ACK_LEN;

	  /**
	   * Position of the window size field.
	   */
	  final int TCP_WIN_POS = TCP_FLAG_POS + TCP_FLAG_LEN;

	  /**
	   * Position of the checksum field.
	   */
	  final int TCP_CSUM_POS = TCP_WIN_POS + TCP_WIN_LEN;

	  /**
	   * Position of the urgent pointer field.
	   */
	  final int TCP_URG_POS = TCP_CSUM_POS + TCP_CSUM_LEN;
 
	  /**
	   * Length in bytes of a TCP header.
	   */
	  final int TCP_HEADER_LEN = TCP_URG_POS + TCP_URG_LEN; // == 20
}
