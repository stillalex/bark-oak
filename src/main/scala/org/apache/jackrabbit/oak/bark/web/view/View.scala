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
package org.apache.jackrabbit.oak.bark.web.view

import scala.collection.JavaConversions.{ asScalaBuffer, iterableAsScalaIterable, seqAsJavaList }
import org.apache.jackrabbit.oak.api.{ PropertyState, Tree, Type }
import org.apache.jackrabbit.oak.bark.web.BaseTemplatePage
import org.apache.jackrabbit.oak.commons.PathUtils
import org.apache.wicket.Component
import org.apache.wicket.markup.html.WebMarkupContainer
import org.apache.wicket.markup.html.basic.Label
import org.apache.wicket.markup.html.form.{ Button, DropDownChoice, RequiredTextField, StatelessForm }
import org.apache.wicket.markup.html.link.BookmarkablePageLink
import org.apache.wicket.markup.html.panel.FeedbackPanel
import org.apache.wicket.markup.repeater.Item
import org.apache.wicket.markup.repeater.data.{ DataView, ListDataProvider }
import org.apache.wicket.model.{ LoadableDetachableModel, Model, PropertyModel }
import org.apache.wicket.request.http.flow.AbortWithHttpErrorCodeException
import org.apache.wicket.request.mapper.parameter.PageParameters
import org.apache.jackrabbit.oak.spi.query.PropertyValues
import org.apache.jackrabbit.oak.namepath.NamePathMapper
import org.apache.wicket.markup.html.form.IChoiceRenderer

class View(pp: PageParameters) extends BaseTemplatePage(pp) {

  def this() = this(null);

  val path: String = if (pp != null) {
    pp.get("p").toString("/");
  } else {
    "/"
  }

  val root: LoadableDetachableModel[Tree] = new LoadableDetachableModel[Tree]() {
    def load(): Tree = {
      if (oakRoot.isEmpty) {
        throw new AbortWithHttpErrorCodeException(404);
      }
      try {
        val t = oakRoot.get.getTree(path);
        if (t.exists()) {
          return t;
        }
        throw new AbortWithHttpErrorCodeException(404);
      } catch {
        case e: IllegalArgumentException ⇒ throw new AbortWithHttpErrorCodeException(404);
      }
    }
  }

  //
  // --

  setStatelessHint(true);
  buildBC(root.getObject(), path);
  buildChildren(root.getObject(), path);
  buildProps(root.getObject());

  add(buildFormContainer().setVisibilityAllowed(!getA.isRO));

  //
  // --

  private[view] def buildBC(root: Tree, path: String) {
    add(new BookmarkablePageLink("root", classOf[View]));
    add(new Label("current", root.getName()));

    val c: List[String] = PathUtils.elements(path).toList.dropRight(1);

    add(new DataView[String]("paths", new ListDataProvider(c)) {

      override def populateItem(item: Item[String]) {
        val p: String = item.getModelObject();
        val link = selfBPL("segment", path.substring(0, path.indexOf(p) + p.length()));
        link.add(new Label("name", p));
        item.add(link);
      }
    });
  }

  private[view] def buildChildren(root: Tree, path: String) {
    val c: List[String] = root.getChildren().map(x ⇒ x.getName()).toList;
    add(new DataView[String]("children", new ListDataProvider(c)) {

      override def populateItem(item: Item[String]) {
        val p: String = item.getModelObject();
        val link = selfBPL("child", PathUtils.concat(path, p));
        link.add(new Label("name", p));
        item.add(link);
      }
    });
  }

  private[view] def selfBPL(id: String, path: String): BookmarkablePageLink[View] = {
    val pp: PageParameters = new PageParameters();
    if (!"/".equals(path)) {
      pp.set("p", path);
    }
    return new BookmarkablePageLink(id, classOf[View], pp);
  }

  private[view] def buildProps(root: Tree) {
    val p: List[(String, String, Type[_])] = root.getProperties().map(x ⇒ (x.getName(), psAsString(x), x.getType())).toList;

    add(new DataView[(String, String, Type[_])]("properties", new ListDataProvider(p)) {

      override def populateItem(item: Item[(String, String, Type[_])]) {
        val p: (String, String, Type[_]) = item.getModelObject();
        item.add(new Label("name", p._1));
        item.add(new Label("value", p._2));
        item.add(new Label("type", p._3.toString()));
      }
    });
  }

  private[view] def psAsString(ps: PropertyState): String = {
    if (ps.isArray()) {
      return "[" + ps.getValue(Type.STRINGS).foldLeft("")((s, v) ⇒ v + ", " + s) + "]";
    }
    return ps.getValue(Type.STRING);
  }

  //
  //--
  //
  var addName: String = "";

  private[view] def buildFormContainer(): Component = {
    val con = new WebMarkupContainer("addFormContainer");
    con.add(buildForm);
    con.add(buildPropertyForm);
    con.add(new FeedbackPanel("feedback"));
    return con;
  }

  private[view] def buildForm(): Component = {
    val form = new StatelessForm[Void]("addForm");
    form.setOutputMarkupId(true);

    val a = new RequiredTextField[String]("add", new PropertyModel[String](
      this, "addName"))
    a.setLabel(new Model("Node name"));

    val submit = new Button("submit") {
      override def onSubmit() =
        try {
          val c = oakRoot.get.getTree(path).addChild(addName);
          c.setProperty("jcr:primaryType", "nt:unstructured", Type.NAME);

          val pp: PageParameters = new PageParameters();
          if (!"/".equals(path)) {
            pp.set("p", path);
          }
          setResponsePage(classOf[View], pp);

        } catch {
          case e: Exception ⇒ {
            e.printStackTrace()
            error(e.getMessage());
          }
        }
    };

    form.add(a);
    form.add(submit);
    form.setDefaultButton(submit);
    return form;
  }

  var addPName: String = "";
  var addPVal: String = "";
  var addPType: Int = Type.STRING.tag;

  private[view] def buildPropertyForm(): Component = {
    val form = new StatelessForm[Void]("addPropertyForm");
    form.setOutputMarkupId(true);

    val n = new RequiredTextField[String]("name", new PropertyModel[String](
      this, "addPName"))
    n.setLabel(new Model("Property name"));
    val v = new RequiredTextField[String]("val", new PropertyModel[String](
      this, "addPVal"))
    v.setLabel(new Model("Property value"));

    val t = new DropDownChoice[Int]("ptype", new PropertyModel[Int](this, "addPType"),
      List[Int](Type.STRING.tag, Type.BOOLEAN.tag, Type.DATE.tag, Type.DECIMAL.tag, Type.DOUBLE.tag, Type.LONG.tag, Type.NAME.tag, Type.REFERENCE.tag, Type.WEAKREFERENCE.tag), new TypeChoiceRenderer());

    val submit = new Button("submit") {
      override def onSubmit() =
        try {
          oakRoot.get.getTree(path).setProperty(addPName, addPVal, Type.fromTag(addPType, false).asInstanceOf[Type[Object]]);
          //            PropertyValues.convert(PropertyValues.newString(addPVal), addPType, NamePathMapper.DEFAULT).getValue(Type.fromTag(addPType, false)), 
          //            Type.fromTag(addPType, false).asInstanceOf[Type[Object]]);

          val pp: PageParameters = new PageParameters();
          if (!"/".equals(path)) {
            pp.set("p", path);
          }
          setResponsePage(classOf[View], pp);

        } catch {
          case e: Exception ⇒ {
            e.printStackTrace()
            error(e.getMessage());
          }
        }
    };

    form.add(n);
    form.add(v);
    form.add(t);
    form.add(submit);
    form.setDefaultButton(submit);
    return form;
  }

  private class TypeChoiceRenderer extends IChoiceRenderer[Int] {
    override def getDisplayValue(id: Int) = Type.fromTag(id, false).toString();
    override def getIdValue(id: Int, index: Int) = id.toString;
  }

}