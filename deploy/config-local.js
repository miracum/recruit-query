define([], function () {
  const configLocal = {};

  // clearing local storage otherwise source cache will obscure the override settings
  localStorage.clear();

  const getUrl = window.location;
  const baseUrl = getUrl.protocol + "//" + getUrl.host;

  // WebAPI
  configLocal.api = {
    name: "OHDSI",
    url: baseUrl + "/WebAPI/",
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
      isUseCredentialsForm: true
    },
  ];

  return configLocal;
});
