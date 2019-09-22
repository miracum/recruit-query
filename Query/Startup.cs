using Hl7.Fhir.Rest;
using Microsoft.AspNetCore.Builder;
using Microsoft.AspNetCore.Hosting;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Hosting;
using Query.Models.Omop;

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
            services.AddTransient<ICohortProvider, OmopCohortProvider>();
            services.AddTransient<IAtlasApiClient, AtlasApiClient>();
            services.AddDbContext<OmopContext>(options => options.UseNpgsql(Configuration.GetConnectionString("OHDSIDatabase")));
            services.AddTransient<IOmopDatabaseClient, OmopConnector>();
            services.AddSingleton<IFhirClient, FhirClient>(sp =>
            {
                var config = sp.GetService<IConfiguration>();
                var client = new FhirClient(config.GetValue<string>("FhirServerUrl"))
                {
                    PreferredFormat = ResourceFormat.Json,
                };
                return client;
            });
            services.AddTransient<IScreeningListService, FhirScreeningListService>();
            services.AddControllers();
        }

        // This method gets called by the runtime. Use this method to configure the HTTP request pipeline.
        public void Configure(IApplicationBuilder app, IWebHostEnvironment env)
        {
            if (env.IsDevelopment())
            {
                app.UseDeveloperExceptionPage();
            }

            app.UseRouting();

            app.UseEndpoints(endpoints =>
            {
                endpoints.MapControllers();
            });
        }
    }
}
