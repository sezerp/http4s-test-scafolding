package com

//import zio.{Task, Unsafe, ZIO}

package object pawelzabczynski {

//  implicit class TestUnsafeOps[A](t: Task[A]) {
//
//    def runUnsafe(): A = {
//      runUnsafe(5 minutes)
//    }
//
//    def runUnsafe(timeout: Duration): A = {
//      Unsafe
//        .unsafe { implicit unsafe =>
//          zio.Runtime.default.unsafe
//            .run(
//              t.timeoutFail(new RuntimeException("The given task exceed timeout"))(timeout)
//                .tapError(e => ZIO.succeed(e.printStackTrace()))
//            )
//            .getOrThrowFiberFailure()
//        }
//        .ensuring(true)
//    }
//  }
}
