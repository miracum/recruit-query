using System.Collections.Generic;
using System.Threading.Tasks;

namespace Query
{
    public interface ICohortProvider
    {
        Task<List<string>> GetAsync(string id);
    }
}
