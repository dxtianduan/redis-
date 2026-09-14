package com.hmdp.utils;

public interface lLock {
   boolean tryLock(long timeoutSec);
   void unlock();
}
