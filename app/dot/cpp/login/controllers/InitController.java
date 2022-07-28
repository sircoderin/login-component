package dot.cpp.login.controllers;

import dot.cpp.login.models.session.entity.Session;
import dot.cpp.login.models.user.entity.User;
import dot.cpp.repository.controllers.RepositoryController;
import java.util.List;
import javax.inject.Inject;
import play.mvc.Result;

public class InitController {
  private final RepositoryController repositoryController;

  @Inject
  public InitController(RepositoryController repositoryController) {
    this.repositoryController = repositoryController;
  }

  public Result init() {
    return repositoryController.init(List.of(User.class, Session.class));
  }
}
