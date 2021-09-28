define([], function () {
  const configLocal = {};

  // clearing local storage otherwise source cache will obscure the override settings
  localStorage.clear();

  // WebAPI
  configLocal.api = {
    name: "OHDSI",
    url: "http://localhost:8083/WebAPI/",
  };

  configLocal.cohortComparisonResultsEnabled = false;
  configLocal.userAuthenticationEnabled = true;
  configLocal.plpResultsEnabled = false;

  configLocal.authProviders = [
    {
      name: "DB Login",
      url: "user/login/db",
      ajax: true,
      icon: "fa fa-openid",
      isUseCredentialsForm: true,
    },
  ];

  return configLocal;
});
