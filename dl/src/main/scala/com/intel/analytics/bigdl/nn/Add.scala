/*
 * Licensed to Intel Corporation under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * Intel Corporation licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intel.analytics.bigdl.nn

import com.intel.analytics.bigdl.nn.abstractnn.TensorModule
import com.intel.analytics.bigdl.tensor.Tensor
import com.intel.analytics.bigdl.tensor.TensorNumericMath.TensorNumeric
import com.intel.analytics.bigdl.utils.RandomGenerator._

import scala.reflect.ClassTag

/**
 * adds a bias term to input data ;
 *
 * @param inputSize size of input data
 */
@SerialVersionUID(4268487849759172896L)
class Add[T: ClassTag](inputSize: Int
  )(implicit ev: TensorNumeric[T]) extends TensorModule[T] {

  val bias = Tensor[T](inputSize)

  val ones : Tensor[T] = Tensor[T]()

  val gradBias : Tensor[T] = Tensor[T](inputSize)

  reset()

  override def reset(): Unit = {
    val stdv = 1 / math.sqrt(bias.size(1))
    bias.apply1(_ => ev.fromType[Double](RNG.uniform(-stdv, stdv)))
    zeroGradParameters()
  }

  override def updateOutput(input: Tensor[T]): Tensor[T] = {
    output.resizeAs(input).copy(input)
    if (input.isSameSizeAs(bias)) {
      output.add(bias)
    } else {
      val batchSize = input.size(1)
      ones.resize(batchSize)
      ones.fill(ev.fromType[Int](1))
      val biasLocal = bias.view(bias.size.product)
      val outputLocal = output.view(batchSize, output.size.product)
      outputLocal.addr(ev.fromType[Int](1), ones, biasLocal)
    }
    output
  }

  override def updateGradInput(input: Tensor[T], gradOutput: Tensor[T]): Tensor[T] = {
    gradInput.resizeAs(gradOutput)
    gradInput.copy(gradOutput)
    gradInput
  }

  override def accGradParameters(input: Tensor[T], gradOutput: Tensor[T],
                                 scale: Double = 1.0): Unit = {

    if (gradBias.size(1) == 1) {
      gradBias(1) = gradBias(1).add(ev.times(ev.fromType[Double](scale), gradOutput.sum()))
    } else {
      if (input.isSameSizeAs(bias)) {
        gradBias.add(ev.fromType[Double](scale), gradOutput)
      } else {
        val gradOutputLocal = gradOutput.view(input.size(1), gradOutput.size.product)
        gradBias.view(gradBias.size().product).addmv(ev.fromType(scale), gradOutputLocal.t(), ones)
      }
    }
  }

  override def zeroGradParameters(): Unit = {
    gradBias.zero()
  }

  override def clearState() : this.type = {
    super.clearState()
    ones.set()
    this
  }

  override def parameters(): (Array[Tensor[T]], Array[Tensor[T]]) = {
    (Array(this.bias), Array(this.gradBias))
  }

  override def toString(): String = {
    s"nn.Add"
  }
}

object Add {
  def apply[@specialized(Float, Double) T: ClassTag](
    inputSize: Int)(implicit ev: TensorNumeric[T]) : Add[T] = {
    new Add[T](inputSize)
  }
}
