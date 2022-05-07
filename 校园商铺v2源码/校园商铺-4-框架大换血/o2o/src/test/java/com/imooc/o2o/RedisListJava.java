package com.imooc.o2o;

import java.util.List;
import redis.clients.jedis.Jedis; 
 public class RedisListJava {
     public static void main(String[] args) {
     //Connecting to Redis server on localhost 
     int a=1; 
     Jedis jedis = new Jedis("127.0.0.1"); 
     //jedis.auth("iop_A9263"); 
     System.out.println("Connection to server sucessfully"); 
     //store data in redis list jedis.lpush("tutorial-list", "Redis"); 
     jedis.lpush("tutorial-list", "Mysql"); 
     // Get the stored data and print it 
     if(jedis.isConnected()) {
         List<String> list = jedis.lrange("tutorial-list", 0 ,5); 
         for(int i = 0; i<list.size(); i++) { 
             System.out.println("Stored string in redis:: "+list.get(i)); 
             } 
       }
     }
 }