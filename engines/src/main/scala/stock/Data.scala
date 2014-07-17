package io.prediction.engines.stock

import io.prediction.BaseParams
import org.saddle._
import org.saddle.index.IndexTime
import com.github.nscala_time.time.Imports._
import breeze.linalg.{ DenseMatrix, DenseVector }
import com.twitter.chill.MeatLocker
//import com.github.nscala_time.time.Imports._

/*
// Use data after baseData.
// Afterwards, slicing uses idx
// Evaluate fromIdx until untilIdx
// Use until fromIdx to construct training data
class EvaluationDataParams(
  val baseDate: DateTime,
  val fromIdx: Int,
  val untilIdx: Int,
  val trainingWindowSize: Int,
  val evaluationInterval: Int,
  val marketTicker: String,
  val tickerList: Seq[String]) extends BaseParams {}

class TrainingDataParams(
  val baseDate: DateTime,
  val untilIdx: Int,
  val windowSize: Int,
  val marketTicker: String,
  val tickerList: Seq[String]) extends BaseParams {}

// Evaluate with data generate up to idx (exclusive). The target data is also
// restricted by idx. For example, if idx == 10, the data-preparator use data to
// at most time (idx - 1).
// EvluationDataParams specifies idx where idx in [fromIdx, untilIdx).
class ValidationDataParams(
  val baseDate: DateTime,
  val fromIdx: Int,
  val untilIdx: Int,
  val marketTicker: String,
  val tickerList: Seq[String]) extends BaseParams {}
*/

class TrainingData(
  val tickers: Seq[String],
  val mktTicker: String,
  val data: (Array[DateTime], Array[(String, Array[Double])]))
  extends Serializable {
  val timeIndex: Array[DateTime] = data._1
  val tickerPriceSeq: Array[(String, Array[Double])] = data._2
 
  @transient lazy val price = SaddleWrapper.ToFrame(timeIndex, tickerPriceSeq)

  override def toString(): String = {
    val firstDate = timeIndex.head
    val lastDate = timeIndex.last
    s"TrainingData [$firstDate, $lastDate]"
  }
}

object TrainingData {
  def apply(
    tickers: Seq[String],
    mktTicker: String,
    price: Frame[DateTime, String, Double]): TrainingData = {
    return new TrainingData(
      tickers,
      mktTicker,
      SaddleWrapper.FromFrame(price))
  }
}

object SaddleWrapper {
  def ToFrame(
    timeIndex: Array[DateTime],
    tickerPriceSeq: Array[(String, Array[Double])]
    ): Frame[DateTime, String, Double] = {
    val index = IndexTime(timeIndex:_ *)
    val seriesList = tickerPriceSeq.map{ case(ticker, price) => {
      val series = Series(Vec(price), index)
      (ticker, series)
    }}
    Frame(seriesList:_*)
  }

  def FromFrame(data: Frame[DateTime, String, Double]
    ): (Array[DateTime], Array[(String, Array[Double])]) = {
    val timeIndex = data.rowIx.toVec.contents
    val tickers = data.colIx.toVec.contents

    val tickerDataSeq = tickers.map{ ticker => {
      (ticker, data.firstCol(ticker).toVec.contents)
    }}

    (timeIndex, tickerDataSeq)
  }
}



class Model(val data: Map[String, DenseVector[Double]]) extends Serializable

// This is different from TrainingData. This serves as input for algorithm.
// Hence, the time series should be shorter than that of TrainingData.
class Feature(
  val mktTicker: String,
  val tickerList: Seq[String],
  val input: (Array[DateTime], Array[(String, Array[Double])]),
  val tomorrow: DateTime
  ) extends Serializable {
  
  val timeIndex: Array[DateTime] = input._1
  val tickerPriceSeq: Array[(String, Array[Double])] = input._2
  @transient lazy val data = SaddleWrapper.ToFrame(timeIndex, tickerPriceSeq)

  def today: DateTime = data.rowIx.last.get

  override def toString(): String = {
    val firstDate = data.rowIx.first.get
    val lastDate = data.rowIx.last.get
    s"Feature [$firstDate, $lastDate]"
  }
}

object Feature {
  def apply(
    mktTicker: String,
    tickerList: Seq[String],
    data: Frame[DateTime, String, Double],
    tomorrow: DateTime): Feature = {
    return new Feature(
      mktTicker = mktTicker,
      tickerList = tickerList,
      input = SaddleWrapper.FromFrame(data),
      tomorrow = tomorrow
      )
  }
}

/*
class Target(
  val data: Map[String, Double]) extends Serializable {}

class ValidationUnit(
  val data: Seq[(Double, Double)]) extends Serializable {}

class ValidationResults(
  val vuSeq: Seq[ValidationUnit]) extends Serializable {}

class CrossValidationResults(val s: String) extends Serializable {
  override def toString() = s
}
*/