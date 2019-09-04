using System.Threading.Tasks;

namespace Query
{
    public interface IAtlasApiClient
    {
        // returns/completes only after the cohort has been completely generated.
        Task GenerateCohortAsync();
    }
}
