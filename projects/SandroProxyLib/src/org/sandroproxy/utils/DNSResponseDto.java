package org.sandroproxy.utils;

public class DNSResponseDto {
    
    
    private String request;
    private long timestamp = System.currentTimeMillis();;
    private int reqTimes = 0;
    private byte[] dnsResponse;
    
    public DNSResponseDto(String request){
        this.request = request;
    }
    
    public byte[] getDNSResponse(){
        reqTimes++;
        return dnsResponse;
    }
    
    public String getIPString() {
        String ip = null;
        int i;

        if (dnsResponse == null) {
            return null;
        }

        i = dnsResponse.length - 4;

        if (i < 0) {
            return null;
        }

        ip = "" + (dnsResponse[i] & 0xFF); /* Unsigned byte to int */

        for (i++; i < dnsResponse.length; i++) {
            ip += "." + (dnsResponse[i] & 0xFF);
        }

        return ip;
    }
    
    /**
     * @return the reqTimes
     */
    public int getReqTimes() {
        return reqTimes;
    }

    public String getRequest() {
        return this.request;
    }

    /**
     * @return the timestamp
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * @param dnsResponse
     *            the dnsResponse to set
     */
    public void setDNSResponse(byte[] dnsResponse) {
        this.dnsResponse = dnsResponse;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("request=").append(request);
        sb.append(", ").append("timestamp=").append(timestamp);
        sb.append(", ").append("reqTimes=").append(reqTimes);
        sb.append(", ").append("dnsResponse=").append(dnsResponse);
        return sb.toString();
    }
}
