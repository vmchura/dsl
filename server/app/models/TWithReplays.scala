package models

trait TWithReplays[A] { this: A =>
  def matchPK: MatchPK
  def withReplays(replays: Seq[ReplayRecord]): A
}
