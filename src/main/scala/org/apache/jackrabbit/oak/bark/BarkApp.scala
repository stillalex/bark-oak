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

import org.apache.jackrabbit.oak.bark.web.home.Index
import org.apache.jackrabbit.oak.bark.web.login.Login
import org.apache.jackrabbit.oak.bark.web.view.View
import org.apache.jackrabbit.oak.bark.web.viewadmin.ViewAdmin
import org.apache.wicket.protocol.http.WebApplication
import org.apache.wicket.request.{ Request, Response }
import com.pfalabs.soak.OakRepository

class BarkApp extends WebApplication with OakRepository {

  override def getHomePage = classOf[Index];

  override def newSession(request: Request, response: Response) = new BaseSession(request, repository);

  override def init() = {
    super.init();

    // markup settings
    getMarkupSettings().setStripWicketTags(true);
    getMarkupSettings().setDefaultMarkupEncoding("UTF-8");

    // page settings
    getPageSettings().setVersionPagesByDefault(false);

    mountPage("/login", classOf[Login]);
    mountPage("/view", classOf[View]);
    mountPage("/viewadmin", classOf[ViewAdmin]);

    initOak("bark-oak");
  }
}
  
