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
package org.apache.jackrabbit.oak.bark.web.login

import org.apache.jackrabbit.oak.bark.web.BaseTemplatePage
import org.apache.wicket.Component
import org.apache.wicket.markup.html.form.{ Button, PasswordTextField, RequiredTextField, StatelessForm }
import org.apache.wicket.markup.html.panel.FeedbackPanel
import org.apache.wicket.model.PropertyModel

class Login extends BaseTemplatePage {

  var user: String = "";
  var pass: String = "";

  def buildForm(): Component = {
    val form = new StatelessForm[Void]("loginForm");
    form.setOutputMarkupId(true);

    val u = new RequiredTextField[String]("u", new PropertyModel[String](
      this, "user"))
    val p = new PasswordTextField("p", new PropertyModel[String](
      this, "pass"));

    val submit = new Button("submit") {
      override def onSubmit() {
        val user: String = u.getModelObject;
        val pass: String = p.getModelObject;
        getA.login(user, pass) match {
          case Right(u) ⇒ {
            continueToOriginalDestination();
            setResponsePage(getApplication().getHomePage());
          }
          case Left(u) ⇒ {
            error(u);
          }
        }
      }
    };

    form.add(u);
    form.add(p);
    form.add(submit);
    form.setDefaultButton(submit);
    return form;
  }

  setStatelessHint(true);
  add(buildForm());
  add(new FeedbackPanel("feedback"));

}