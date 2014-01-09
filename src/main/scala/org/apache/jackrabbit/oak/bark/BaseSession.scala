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

class BaseSession(r: Request, repository: Option[ContentRepository]) extends WebSession(r) {

  var session: Option[ContentSession] = guestSession(repository);

  var root: Option[Root] = latestRoot(session);

  def isRO(): Boolean = isReadOnly(session);

  def login(u: String, p: String): Either[String, ContentSession] = login(repository.get, u, p) match {
    case Right(u) ⇒ {
      session = Some(u);
      root = latestRoot(session);
      return Right(u);
    }
    case Left(u) ⇒ {
      return Left(u);
    }
  }

  def logout() = if (!isReadOnly(session)) {
    session = guestSession(repository);
    root = latestRoot(session);
  }

  // ----------------------------------------------------
  // OAK SESSION
  // ----------------------------------------------------

  private def latestRoot(session: Option[ContentSession]): Option[Root] =
    session match {
      case Some(s) ⇒ Some(s.getLatestRoot());
      case None ⇒ None;
    }

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
