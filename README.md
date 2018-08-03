# jogging
Backend server REST API that tracks jogging times of users.

Features:
<ul>
<li>API is able to create an account and log in.</li>
<li>All API calls are authenticated.</li>
<li>There are three roles with different permission levels: a regular user would only be able to CRUD on their owned records, a user manager would be able to CRUD only users, and an admin would be able to CRUD all records and users.</li>
<li>Each time entry when entered has a date, distance, time, and location.</li>
<li>Based on the provided date and location, API do connect to a weather API provider and get the weather conditions for the run, and store that with each run.</li>
<li>The API allows to create a report on average speed & distance per week.</li>
<li>The API is able to return data in the JSON format.</li>
<li>The API provides filter capabilities for all endpoints that return a list of elements, as well should be able to support pagination.</li>
<li>The API filtering allow using parenthesis for defining operations precedence and use any combination of the available fields.
The supported operations include 'or', 'and', 'eq' (equals), 'ne' (not equals), 'gt' (greater than), 'lt' (lower than), 'like'.
Example -> (date eq '2016-05-01') AND ((distance gt 20) OR (distance lt 10)).</li>
</ul>

