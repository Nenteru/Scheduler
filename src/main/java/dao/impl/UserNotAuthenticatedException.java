package dao.impl;

import java.io.IOException;

/**
* Исключение, выбрасываемое, когда пользователь пытается получить доступ к Google Calendar,
* не будучи аутентифицированным.
*/
public class UserNotAuthenticatedException extends IOException {
   public UserNotAuthenticatedException(String message) {
       super(message);
   }

   public UserNotAuthenticatedException(String message, Throwable cause) {
       super(message, cause);
   }
}