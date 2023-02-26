package com

import org.http4s.Query

//import zio.{Task, Unsafe, ZIO}

package object pawelzabczynski {
  implicit class RichQuery(q: Query) {
    def has(name: String, value: String): Boolean = {
      q.exists {
        case (k, v) => k == name && v.contains(value)
      }
    }
  }
}
