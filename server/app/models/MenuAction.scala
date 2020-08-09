package models

trait MenuAction{
  def tag: String
  def url: String
}
case class MenuActionDefined(tag: String, url: String) extends MenuAction
case class MenuGroup(tag: String, submenus: Seq[MenuAction])
