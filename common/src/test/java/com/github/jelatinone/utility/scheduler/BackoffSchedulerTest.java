package com.github.jelatinone.utility.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BackoffSchedulerTest {

  @Test
  void constantScheduler_alwaysReturnsBaseBackoff() {
    ConstantBackoffScheduler scheduler = new ConstantBackoffScheduler(25);

    assertEquals(25, scheduler.compute());
    assertEquals(25, scheduler.compute(3));
    assertEquals(25, scheduler.acquire());
    assertEquals(25, scheduler.reset());
  }

  @Test
  void linearScheduler_advancesAttempts_and_resets() {
    LinearBackoffScheduler scheduler = new LinearBackoffScheduler(10, 30, 5);

    assertEquals(10, scheduler.acquire());
    assertEquals(10, scheduler.compute());
    assertTrue(scheduler.compute() >= 10 && scheduler.acquire() <= 15);
    assertTrue(scheduler.compute(3) >= 10 && scheduler.acquire() <= 25);

    long previous = scheduler.reset();
    assertTrue(previous >= 10);
    assertEquals(10, scheduler.acquire());
    assertTrue(scheduler.compute() >= 10 && scheduler.acquire() <= 10);
  }

  @Test
  void exponentialScheduler_isSafeOnFirstCompute_andSupportsStepComputes() {
    ExponentialBackoffScheduler scheduler = new ExponentialBackoffScheduler(10, 100, 2);

    assertEquals(10, scheduler.acquire());
    assertTrue(scheduler.compute() >= 10 && scheduler.acquire() <= 20);
    assertTrue(scheduler.compute(3) >= 10 && scheduler.acquire() <= 80);

    long previous = scheduler.reset();
    assertTrue(previous >= 10);
    assertEquals(10, scheduler.acquire());
  }
}
