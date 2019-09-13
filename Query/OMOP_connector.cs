using System;
using System.Collections;
using System.Collections.Generic;

namespace Query
{
    /// <summary>
    /// TODO: Beschreibung.
    /// </summary>
    public class OMOP_connector
    {
        /// <summary>
        /// Checks if database configurations specified in app.config are valid and database is of OMOP schema.
        /// </summary>
        /// <returns> true if valid, false if not.</returns>
        public static bool CheckConnection()
        {
            return true;
        }

        /// <summary>
        /// Requests all Patient IDs connected to the given cohort.
        /// </summary>
        /// <param name="cohortId"> Id of the requested cohort.</param>
        /// <returns> ArrayList of Ids.</returns>
        public static ArrayList GetIdsFromCohort(string cohortId)
        {
            return new ArrayList();
        }
    }
}
