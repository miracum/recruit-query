﻿using Hl7.Fhir.Rest;
using Microsoft.AspNetCore.Builder;
using Microsoft.AspNetCore.Hosting;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Query.Models.Omop;
using RestSharp;

#pragma warning disable CS1591, SA1600

namespace Query
{
    public class Startup
    {
        public Startup(IConfiguration configuration)
        {
            Configuration = configuration;
        }

        public IConfiguration Configuration { get; }

        // This method gets called by the runtime. Use this method to add services to the container.
        public void ConfigureServices(IServiceCollection services)
        {
            services.AddDbContext<OmopContext>(
                options => options.UseNpgsql(Configuration.GetConnectionString("OmopDatabase")),
                ServiceLifetime.Singleton);

            services.AddTransient<ICohortProvider, OmopCohortProvider>();
            services.AddTransient<IAtlasApiClient, AtlasApiClient>();
            services.AddTransient<IOmopDatabaseClient, OmopDatabaseClient>();
            services.AddTransient<IScreeningListService, FhirScreeningListService>();

            services.AddSingleton<IFhirClient>(sp =>
            {
                var config = sp.GetService<IConfiguration>();
                var client = new FhirClient(config.GetValue<string>("FhirBaseUrl"))
                {
                    PreferredFormat = ResourceFormat.Json,
                };
                return client;
            });

            services.AddSingleton<IRestClient>(sp =>
            {
                var config = sp.GetService<IConfiguration>();
                return new RestClient(config.GetValue<string>("OhdsiWebApiBaseUrl"));
            });

            services.AddHostedService<BackgroundWorker>();
        }
        
        public void Configure(IApplicationBuilder app, IWebHostEnvironment env)
        {
            if (env.IsDevelopment())
            {
                app.UseDeveloperExceptionPage();
            }
        }

    }
}
