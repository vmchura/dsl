package models

trait MenuAction {
  def tag: String
  def url: String
}
case class MenuActionDefined(tag: String, url: String) extends MenuAction
case class ExtraAction(tag: String, url: String) extends MenuAction
case class MenuGroup(tag: String, submenus: Seq[MenuAction]) {
  def primaryActions(): Seq[MenuActionDefined] =
    submenus.flatMap {
      case m @ MenuActionDefined(_, _) => Some(m)
      case _                           => None
    }
  def secondaryActions(): Seq[ExtraAction] =
    submenus.flatMap {
      case m @ ExtraAction(_, _) => Some(m)
      case _                     => None
    }
  def hasSecondaryActions: Boolean = secondaryActions().nonEmpty
}
