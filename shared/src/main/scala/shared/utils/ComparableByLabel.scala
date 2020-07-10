package shared.utils

trait ComparableByLabel {
  def stringLabel: String
  final def stringLabelNormalized: String = stringLabel.filter(_.isLetterOrDigit).toLowerCase()
}
