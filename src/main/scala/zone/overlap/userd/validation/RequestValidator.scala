// Copyright 2017 Vy-Shane Xie Sin Fat

package zone.overlap.userd.validation

import cats.data.Validated.{Invalid, Valid}
import cats.data.ValidatedNel
import cats.implicits._
import io.grpc.Status
import monix.eval.Task
import zone.overlap.api.{SignUpRequest, UpdateInfoRequest}
import zone.overlap.privateapi.FindUserByIdRequest

sealed trait RequestValidator {

  type ValidationResult[A] = ValidatedNel[UserValidation, A]

  def ensureValid[A](validator: A => ValidationResult[A])(value: A): Task[A] =
    validator(value) match {
      case Valid(value) => Task(value)
      case Invalid(nel) => {
        val errorDescription = nel.map(v => v.errorMessage).toList.mkString("\n")
        Task.raiseError(Status.INVALID_ARGUMENT.augmentDescription(errorDescription).asRuntimeException())
      }
    }

  def validateFindUserByIdRequest(request: FindUserByIdRequest): ValidationResult[FindUserByIdRequest] = {
    if (request.userId.isEmpty) UserIdIsRequired.invalidNel
    else request.valid
  }

  def validateSignUpRequest(emailExists: String => Boolean)(request: SignUpRequest): ValidationResult[SignUpRequest] = {
    (validateFirstName(request.firstName),
     validateLastName(request.lastName),
     validateEmail(emailExists)(request.email),
     validatePassword(request.password)).mapN(SignUpRequest.apply)
  }

  def validateUpdateUserInfoRequest(request: UpdateInfoRequest): ValidationResult[UpdateInfoRequest] = {
    (validateFirstName(request.firstName), validateLastName(request.lastName)).mapN(UpdateInfoRequest.apply)
  }

  private[validation] def validateFirstName(firstName: String): ValidationResult[String] =
    if (firstName.length > 1 && firstName.length <= 255) firstName.validNel
    else FirstNameIsInvalid.invalidNel

  private[validation] def validateLastName(lastName: String): ValidationResult[String] =
    if (lastName.length > 1 && lastName.length <= 255) lastName.validNel
    else LastNameIsInvalid.invalidNel

  private[validation] def validateEmail(emailExists: String => Boolean)(email: String): ValidationResult[String] =
    if (email.isEmpty) EmailIsRequired.invalidNel
    else if (!validateEmailFormat(email)) EmailIsInvalid.invalidNel
    else if (emailExists(email)) EmailIsTaken.invalidNel
    else email.validNel

  private def validateEmailFormat(email: String): Boolean = {
    // Regex source: https://www.w3.org/TR/html5/forms.html#valid-e-mail-address
    """^[a-zA-Z0-9\.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9]|(?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$""".r
      .findFirstMatchIn(email)
      .isDefined
  }

  private[validation] def validatePassword(password: String): ValidationResult[String] =
    if (password.length >= 6) password.validNel
    else PasswordIsTooShort.invalidNel
}

object RequestValidator extends RequestValidator
