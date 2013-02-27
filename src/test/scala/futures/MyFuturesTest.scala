package test.scala.futures

import org.scalatest.FunSuite
import scala.concurrent.future
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

import work._
import futures.MyFutures

class MyFuturesTest extends FunSuite {
  val FUTURE_TIME_LIMIT: Long = 100

  def delayFactorNumber(n: Long): FactorNumber = new FactorNumber(n, FUTURE_TIME_LIMIT * 2)

  test("should compute square") {
    val future = time {
      MyFutures.computeSquare(2)
    }
    val result = Await.result(future, Duration.Inf)
    assert(result == 4)
  }

  test("should compute square of future value") {
    val futureValue = future {
      Thread.sleep(FUTURE_TIME_LIMIT * 2)
      2
    }
    val futureResult = time {
      MyFutures.computeSquare(futureValue)
    }
    val result = Await.result(futureResult, Duration.Inf)
    assert(result == 4)
  }

  test("should find max factor") {
    val work = delayFactorNumber(49L)
    val futureResult = time {
      MyFutures.findMaxFactor(work)
    }
    val result = Await.result(futureResult, Duration.Inf)
    assert(result == 7L)
  }

  test("should find max factor of future factors") {
    val futureFactors = future {
      delayFactorNumber(49L)
    }
    val futureResult = time {
      MyFutures.findMaxFactor(futureFactors)
    }
    val result = Await.result(futureResult, Duration.Inf)
    assert(result == 7L)
  }

  test("do risky work or fallback on safe work") {
    // Each work will exceed the time limit
    val shouldNotDoWork = new SumSequence(0, 4, FUTURE_TIME_LIMIT + 1)
    val safeWork = new SumSequence(0, 5, FUTURE_TIME_LIMIT + 1)
    val riskyWork = new SumSequence(-1, 6, FUTURE_TIME_LIMIT + 1)
    val futureSafeResult = time {
      MyFutures.computeRiskySumFallbackOnSafeSum(safeWork, shouldNotDoWork)
    }
    val futureSafeResult2 = time {
      MyFutures.computeRiskySumFallbackOnSafeSum(riskyWork, safeWork)
    }

    val result = Await.result(futureSafeResult, Duration.Inf)
    val result2 = Await.result(futureSafeResult2, Duration.Inf)
    assert(result == 15)
    assert(result2 == 15)
  }

  test("find sum of all max factors") {
    val work = Seq(delayFactorNumber(21L), delayFactorNumber(49L), delayFactorNumber(12L))
    val futureResult = time {
      MyFutures.findSumOfAllMaxFactors(work)
    }
    val result = Await.result(futureResult, Duration.Inf)
    assert(result == 20L)
  }

  test("find max factor of all max factors in parallel") {
    // Each work will take at least 100 milliseconds
    val work = Seq(delayFactorNumber(49L), delayFactorNumber(12L), delayFactorNumber(21L), delayFactorNumber(54L))

    val futureResult = time {
      MyFutures.findMaxFactorOfAllMaxFactorsInParallel(work)
    }
    val t1 = System.currentTimeMillis()
    val result = Await.result(futureResult, Duration.Inf)
    assert(result == 27)
    val totalExecutionTime = System.currentTimeMillis() - t1
    assert(totalExecutionTime < FUTURE_TIME_LIMIT * 7)
    println("Parallel execution time: " + totalExecutionTime)
  }

  private def time[R](block: => R): R = {
    val t0 = System.currentTimeMillis()
    val result = block
    val t1 = System.currentTimeMillis()
    if (t1 - t0 > FUTURE_TIME_LIMIT)
      fail("Did not return immediately as a future!")
    result
  }

}

