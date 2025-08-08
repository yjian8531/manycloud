package com.core.manycloudcommon.utils;

import java.net.InetAddress;

public class DNSTest {
    public static void main(String[] args) {
        String domain = "sellercentral.amazon.com";
        try{
            InetAddress[] addresses = InetAddress.getAllByName(domain);
            for (InetAddress address : addresses) {
                System.out.println(address);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        /*String[] dnsServers = { "23.62.20.110", "8.8.8.8", "104.109.129.170", "23.219.172.184", "23.3.104.205", "104.109.129.169","52.84.251.53"};
        for (String dnsServer : dnsServers) {
            try {
                InetAddress[] addresses = InetAddress.getAllByName(dnsServer);
                for (InetAddress address : addresses) {
                    long start = System.nanoTime();
                    InetAddress.getAllByName(domain);
                    long end = System.nanoTime();
                    long duration = TimeUnit.NANOSECONDS.toMillis(end - start);
                    System.out.printf("DNS Server: %s, Response Time: %d ms%n", address, duration);
                }
            } catch (UnknownHostException e) {
                System.err.println("DNS server not found: " + e.getMessage());
            }
        }*/
    }
}