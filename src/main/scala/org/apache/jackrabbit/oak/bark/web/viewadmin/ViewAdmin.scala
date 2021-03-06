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
package org.apache.jackrabbit.oak.bark.web.viewadmin

import scala.collection.JavaConversions.iterableAsScalaIterable
import scala.collection.JavaConversions.seqAsJavaList
import org.apache.jackrabbit.oak.api.PropertyState
import org.apache.jackrabbit.oak.api.Type
import org.apache.jackrabbit.oak.bark.web.BaseTemplatePage
import org.apache.jackrabbit.oak.commons.PathUtils
import org.apache.jackrabbit.oak.plugins.memory.PropertyStates
import org.apache.jackrabbit.oak.spi.state.NodeState
import org.apache.wicket.Component
import org.apache.wicket.markup.html.WebMarkupContainer
import org.apache.wicket.markup.html.basic.Label
import org.apache.wicket.markup.html.form.Button
import org.apache.wicket.markup.html.form.DropDownChoice
import org.apache.wicket.markup.html.form.IChoiceRenderer
import org.apache.wicket.markup.html.form.RequiredTextField
import org.apache.wicket.markup.html.form.StatelessForm
import org.apache.wicket.markup.html.link.BookmarkablePageLink
import org.apache.wicket.markup.html.panel.FeedbackPanel
import org.apache.wicket.markup.repeater.Item
import org.apache.wicket.markup.repeater.data.DataView
import org.apache.wicket.markup.repeater.data.ListDataProvider
import org.apache.wicket.model.LoadableDetachableModel
import org.apache.wicket.model.Model
import org.apache.wicket.model.PropertyModel
import org.apache.wicket.request.http.flow.AbortWithHttpErrorCodeException
import org.apache.wicket.request.mapper.parameter.PageParameters
import javax.jcr.PropertyType
import org.apache.wicket.markup.html.link.StatelessLink
import org.apache.wicket.model.IModel

class ViewAdmin(pp: PageParameters) extends BaseTemplatePage(pp) {

  def this() = this(null);

  if (!isAdmin) {
    throw new AbortWithHttpErrorCodeException(403);
  }

  def isAdmin(): Boolean = "admin".equals(oakSession.getAuthInfo().getUserID())

  val path: String = if (pp != null) {
    pp.get("p").toString("/");
  } else {
    "/"
  }

  val root: LoadableDetachableModel[NodeState] = new LoadableDetachableModel[NodeState]() {
    def load(): NodeState = {
      try {
        println(path)
        var ns: NodeState = oakNodeStore.getRoot()
        PathUtils.elements(path).foreach(p ⇒ ns = ns.getChildNode(p))
        if (ns.exists()) {
          return ns;
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

  add(buildFormContainer().setVisibilityAllowed(!getS.isRO));

  //
  // --

  private def buildBC(root: NodeState, path: String) {
    add(new BookmarkablePageLink("root", classOf[ViewAdmin]));
    add(new Label("current", PathUtils.getName(path)));

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

  private def buildChildren(root: NodeState, path: String) {
    add(new DataView[String]("children", new ListDataProvider(root.getChildNodeNames().toList)) {
      override def populateItem(item: Item[String]) {
        val p: String = item.getModelObject();
        val link = selfBPL("child", PathUtils.concat(path, p));
        link.add(new Label("name", p));
        item.add(link);
      }
    });
  }

  private def selfBPL(id: String, path: String): BookmarkablePageLink[ViewAdmin] = {
    val pp: PageParameters = new PageParameters();
    if (!"/".equals(path)) {
      pp.set("p", path);
    }
    return new BookmarkablePageLink(id, classOf[ViewAdmin], pp);
  }

  private def buildProps(root: NodeState) {
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

  private def psAsString(ps: PropertyState): String = {
    if (ps.isArray()) {
      return "[" + ps.getValue(Type.STRINGS).foldLeft("")((s, v) ⇒ v + ", " + s) + "]";
    }
    return ps.getValue(Type.STRING);
  }

  //
  //--
  //
  var addName: String = "";

  private def buildFormContainer(): Component = {
    val con = new WebMarkupContainer("addFormContainer");
    con.add(buildForm);
    con.add(buildPropertyForm);
    con.add(new FeedbackPanel("feedback"));
    return con;
  }

  private def buildForm(): Component = {
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
          setResponseToMe();
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

  private def buildPropertyForm(): Component = {
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
          val p: PropertyState = addPType match {
            case PropertyType.STRING ⇒ PropertyStates.createProperty(addPName, addPVal, addPType);
            case PropertyType.BOOLEAN ⇒ PropertyStates.createProperty(addPName, addPVal, addPType);
            case PropertyType.DECIMAL ⇒ PropertyStates.createProperty(addPName, addPVal, addPType);
            case PropertyType.DOUBLE ⇒ PropertyStates.createProperty(addPName, addPVal, addPType);
            case PropertyType.LONG ⇒ PropertyStates.createProperty(addPName, addPVal, addPType);
            case PropertyType.DATE ⇒ PropertyStates.createProperty(addPName, addPVal, addPType);

            case PropertyType.NAME ⇒ PropertyStates.createProperty(addPName, addPVal, Type.NAME);
            case PropertyType.REFERENCE ⇒ PropertyStates.createProperty(addPName, addPVal, Type.REFERENCE);
            case PropertyType.WEAKREFERENCE ⇒ PropertyStates.createProperty(addPName, addPVal, Type.WEAKREFERENCE);
          }

          oakRoot.get.getTree(path).setProperty(p);
          setResponseToMe();
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

  def setResponseToMe() = {
    val pp: PageParameters = new PageParameters();
    if (!"/".equals(path)) {
      pp.set("p", path);
    }
    setResponsePage(classOf[ViewAdmin], pp);
  }

  // ---------------------------------------------------------------------------------------------------

  add(new WebMarkupContainer("dirty").setVisibilityAllowed(oakRoot.isDefined && oakRoot.get.hasPendingChanges()));

  add(new StatelessLink("commit") {
    override def onClick() =
      oakRoot match {
        case Some(r) ⇒ { r.commit(); setResponseToMe(); }
        case _ ⇒ ;
      }
  });

  add(new StatelessLink("rebase") {
    override def onClick() =
      oakRoot match {
        case Some(r) ⇒ { r.rebase(); setResponseToMe(); }
        case _ ⇒ ;
      }
  });

  add(new StatelessLink("refresh") {
    override def onClick() =
      oakRoot match {
        case Some(r) ⇒ { r.refresh(); setResponseToMe(); }
        case _ ⇒ ;
      }
  });

  // ---------------------------------------------------------------------------------------------------

  private class TypeChoiceRenderer extends IChoiceRenderer[Int] {
    override def getDisplayValue(id: Int) = Type.fromTag(id, false).toString();
    override def getIdValue(id: Int, index: Int) = id.toString;
    def getObject(id: String, choices: IModel[_ <: java.util.List[_ <: Int]]): Int = {
      id.toInt
    }
  }

}