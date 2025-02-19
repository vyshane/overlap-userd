// Copyright 2017 Vy-Shane Xie Sin Fat

package zone.overlap.userd.validation

sealed trait UserValidation {
  def errorMessage: String
}

case object UserIdIsRequired extends UserValidation {
  override def errorMessage: String = "User ID is required"
}

case object FirstNameIsInvalid extends UserValidation {
  override def errorMessage: String = "First name must be between 1 and 255 characters"
}

case object LastNameIsInvalid extends UserValidation {
  override def errorMessage: String = "Last name must be between 1 and 255 characters"
}

case object EmailIsRequired extends UserValidation {
  override def errorMessage: String = "Email address is required"
}

case object EmailIsInvalid extends UserValidation {
  override def errorMessage: String = "Email address is invalid"
}

case object EmailIsTaken extends UserValidation {
  override def errorMessage: String = "Email address is already taken"
}

case object PasswordIsTooShort extends UserValidation {
  override def errorMessage: String = "Password must be at least 6 characters"
}
