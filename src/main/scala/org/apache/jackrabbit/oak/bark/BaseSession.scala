/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.oak.bark

import org.apache.wicket.protocol.http.WebSession
import org.apache.wicket.request.Request
import org.apache.jackrabbit.oak.api.ContentSession
import org.apache.jackrabbit.oak.api.Root
import org.apache.jackrabbit.oak.api.ContentRepository
import javax.jcr.GuestCredentials
import javax.jcr.SimpleCredentials
import com.pfalabs.soak.Sessions._
import scala.util.Try
import scala.util.Success

class BaseSession(r: Request, repository: ContentRepository) extends WebSession(r) {

  var session: Try[ContentSession] = asGuest(repository)

  var root: Option[Root] = latestRoot(session)

  var isRO: Boolean = true

  def login(u: String, p: String): Either[String, ContentSession] = login(repository, u, p) match {
    case Right(u) ⇒ {
      session = Success(u);
      root = latestRoot(session);
      isRO = false;
      return Right(u);
    }
    case Left(u) ⇒ {
      return Left(u);
    }
  }

  def logout() = {
    session = asGuest(repository);
    root = latestRoot(session);
    isRO = true;
  }

  // ----------------------------------------------------
  // OAK SESSION
  // ----------------------------------------------------

  private def latestRoot(session: Try[ContentSession]): Option[Root] =
    session.toOption.map(_.getLatestRoot)

  def login(repo: ContentRepository, u: String, p: String): Either[String, ContentSession] =
    try {
      return Right(repo.login(new SimpleCredentials(u, p.toCharArray()), null));
    } catch {
      case e: Exception ⇒ {
        e.printStackTrace()
        Left(e.getMessage());
      }
    }

}
