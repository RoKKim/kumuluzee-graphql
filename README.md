# KumuluzEE GraphQL

![KumuluzEE CI](https://github.com/kumuluz/kumuluzee-graphql/workflows/KumuluzEE%20CI/badge.svg)

> Kick-start your GraphQL server development.

KumuluzEE GraphQL project enables you to easily create your own GraphQL server with a few simple annotations and is
fully compliant with [MicroProfile GraphQL Sepcification](https://github.com/eclipse/microprofile-graphql). Using
this extension requires understanding of the basic GraphQL concepts.

Read about GraphQL: [GraphQL](http://graphql.org/learn/).

This project is built upon the [SmallRye GraphQL implementation](https://github.com/smallrye/smallrye-graphql).

> **For 1.0.x users, see the following
README: [kumuluzee-graphql](https://github.com/kumuluz/kumuluzee-graphql/tree/master/core)**
>
> For new users, using MicroProfile based implementation (this README) is recommended.

## Usage

You can enable KumuluzEE GraphQL by adding the following dependency to the project:

```xml
<dependency>
    <groupId>com.kumuluz.ee.graphql</groupId>
    <artifactId>kumuluzee-graphql-mp</artifactId>
    <version>${kumuluzee-graphql.version}</version>
</dependency>
```

When KumuluzEE GraphQL is included in the project, you can start developing your GraphQL services.

### Registering GraphQL Resource

The `@GraphQLApi` annotation must be used on the classes that define GraphQL related functions (queries, mutations,
etc.).
All GraphQL annotated functions in annotated classes will be added to your GraphQL schema.

```java
@GraphQLApi
public class CustomerResource {...}
```

### Defining GraphQL queries

The `@Query` annotation will register your Java function as a Query function in GraphQL. All types and
parameters will be automatically converted to GraphQL types and added to the schema. You can override the query name
(which defaults to the function name without `get` or `set` prefix) or add a description to the query.

```java
@GraphQLApi
public class HelloWorld {

    @Query("helloWorld")
    public String hello() {
        return "Hello world!";
    }

    @Query
    @Name("greet")
    @Description("Greets person.")
    public String sayHello(String person) {
        return "Hello " + person + "!";
    }
}
```

### Defining GraphQL mutations

The `@Mutation` annotation is used for defining mutations. It is used the same way as `@Query` annotation.
The only difference is, that mutations are used for changing persistent state, while queries only retrieve data.

More information on this can be found in GraphQL
documentation: [Queries and mutations](http://graphql.org/learn/queries/).

```java
@GraphQLApi
public class CustomerResource {

    // ...

    @Mutation
    public Customer saveCustomer(Customer customer) {
        return customerService.save(customer);
    }

    @Mutation
    @Name("saveOrder")
    @Description("Saves the order to the database.")
    public String newOrder(Order order) {
        return orderService.save(order);
    }
}
```

### Annotating GraphQL arguments

The name of GraphQL argument (on query or mutation) can be overridden with `@Name` annotation. It can also be marked as
non-nullable with `@NonNull` annotation or assigned a default value with `@DefaultValue` annotation.

```java
@Query
public Integer getCustomerCount(@Name("onlyRegistered") Boolean registered) {
    return customerService.getCustomerCount(registered);
}
```

> Avoid using primitive types as parameters (int, double...), because they cannot be `null`. If you use them, please
> provide their default values with `@DefaultValue` annotation.

### Annotation `@Ignore`

This annotation can be used to ignore a certain field.

```java
public class Customer {
    @Ignore
    private String address;
}
```

### Annotation `@NonNull`

If you want to mark a parameter as required, you can annotate the type with `@NonNull` annotation.
It can be also used on lists:

```java
// non null list of non null students
@NonNull List<@NonNull Student>

@Mutation
public String someMutation(@NonNull String field) {
    return field;
} 
```

### Annotation `@Source`

The `@Source` annotation can be used to define a resolver function for additional fields. The example below adds a
new field `referrer` (of type `String`) on the `Customer` type:

```java
@GraphQLApi
public class CustomerResource {

    @Name("referrer")
    public String getReferrerForCustomer(@Source Customer customer) {
        return refererApi.getReferer(customer);
    }
}
```

The `@Source` annotation can also be used to resolve fields in batches. This is commonly referred to as the dataloader
pattern and is used to solve the N+1 problem. The following example would generate exactly the same schema as the
example
above. The only difference is that in the example above the method is called once for every customer returned and in the
following example the method is called once for all customers that are returned.

```java
@GraphQLApi
public class CustomerResource {

    @Name("referrer")
    public List<String> getReferrerForCustomer(@Source List<Customer> customers) {
        return refererApi.getReferersForMultipleCustomers(customers);
    }
}
```

Another use of the `@Source` annotation is defining nested queries on types. For example:

```java
@GraphQLApi
public class CustomerResource {

    @Name("paidOrders")
    public List<Order> getPaidOrders(@Source Customer customer) {
        return customer.getOrders().stream()
                    .filter(o -> o.isPaid()).collect(Collectors.toList());
    }
}
```

Nested queries can also be batched (dataloader pattern). This will generate the same schema (and functionality) as the
example above:

```java
@GraphQLApi
public class CustomerResource {

    @Name("paidOrders")
    public List<List<Order>> getPaidOrders(@Source List<Customer> customers) {
        return customers.stream.map(c -> c.getOrders().stream()
                        .filter(o -> o.isPaid()).collect(Collectors.toList()))
                    .collect(Collectors.toList());
    }
}
```

### Error handling

Exceptions can be thrown during query/mutation execution. The response will have the structure of the GraphQL error as
defined in the GraphQL specification.

By default, all messages from unchecked exceptions (except some defaults, see below) will be hidden for security
reasons. You can override this behavior with the configuration key `kumuluzee.graphql.exceptions.show-error-message`.
The message will be replaced with `Server Error` and can be set using the configuration
key `kumuluzee.graphql.exceptions.default-error-message`. By default, all messages from checked exceptions will be
shown. You can hide messages from exceptions with the configuration
key `kumuluzee.graphql.exceptions.hide-error-message`. Example configuration:

```yaml
kumuluzee:
  graphql:
    exceptions:
      hide-error-message:
        - com.example.exceptions.HiddenCheckedException
      show-error-message:
        - com.example.exceptions.ShownRuntimeException
      default-error-message: Server error, for more information contact ustomer service.
```

#### `show-error-message` defaults

To provide a more seamless integration with __kumuluzee-rest__, some exceptions are added to `show-error-message`
list by default, namely:

- com.kumuluz.ee.rest.exceptions.InvalidEntityFieldException
- com.kumuluz.ee.rest.exceptions.InvalidFieldValueException
- com.kumuluz.ee.rest.exceptions.NoGenericTypeException
- com.kumuluz.ee.rest.exceptions.NoSuchEntityFieldException
- com.kumuluz.ee.rest.exceptions.QueryFormatException

To disable these defaults and handle everything manually use the following configuration:

```yaml
kumuluzee:
  graphql:
    exceptions:
      include-show-error-defaults: false
```

## Querying GraphQL endpoint

GraphQL endpoint (`/graphql`) should be queried using a POST request. Request body should be a JSON object containing
field `query` with the query that should be excecuted and optionally a field `variables` containing a map of GraphQL
variables. For example:

```json
HTTP POST localhost:8080/graphql
Header: Content-Type: application/json
Post data: 
{
	"query": "{customers {id, name, orders {id, total}}}",
	"variables": {"myVariable": "someValue"}
}
```

### Querying GraphQL schema

GraphQL schema generated from annotations can be accessed by sending a GET request on `/graphql/schema.graphql`
endpoint. By default, some elements from the schema are omitted for readability. Additional information can be added to
schema by setting the following configuration keys to `true`:

```yaml
kumuluzee:
  graphql:
    schema:
      include-scalars: true
      include-schema-definition: true
      include-directives: true
      include-introspection-types: true
```

| `kumuluzee-graphql` configuration | Corresponding `smallrye-graphql` configuration      | Description                               | Default Value |
|:----------------------------------|:----------------------------------------------------|:------------------------------------------|:--------------|
| `include-scalars`                 | `smallrye.graphql.schema.includeScalars`            | Include Scalar definitions in the schema  | `true`        |
| `include-schema-definition`       | `smallrye.graphql.schema.includeSchemaDefinition`   | Include Schema definition                 | `false`       |
| `include-directives`              | `smallrye.graphql.schema.includeDirectives`         | Include directives in the schema          | `false`       |
| `include-introspection-types`     | `smallrye.graphql.schema.includeIntrospectionTypes` | Include Introspection types in the schema | `false`       |

For more detailed information on configuration refer to
the [SmallRye GraphQL documentation](https://smallrye.io/smallrye-graphql/2.7.0/server_configuration/#from-smallrye-graphql).

### GraphQL endpoint mapping

To extend the documentation with information about setting the GraphQL endpoint mapping to the root (`/`), you can
include an additional paragraph and modify the YAML configuration example. This shows how the mapping can be customized
or set to the root, depending on the desired endpoint structure. Below is the revised documentation section
incorporating this update:

---

### GraphQL endpoint mapping

The GraphQL server and schema will be served on `/graphql/` by default. You can change this with the KumuluzEE
configuration framework by setting the following key:

```yaml
kumuluzee:
  graphql:
    mapping: customers-api
```

This configuration maps the GraphQL endpoint to `/customers-api/`. If you prefer to serve the GraphQL server and schema
from the root (`/`), you can set the `mapping` key to an empty value as shown below:

```yaml
kumuluzee:
  graphql:
    mapping: /
```

By setting the `mapping` key to `/`, the GraphQL endpoint will be accessible directly from the root URL of your
application, simplifying the URL structure for clients that interact with your GraphQL API.

## Annotation scanning for GraphQL schema generation

By default KumuluzEE GraphQL uses optimized scanning in order to reduce startup times. This means that only the main
application JAR will be scanned (main artifact). In order to scan additional artifacts you need to specify them using
the [scan-libraries mechanism](https://github.com/kumuluz/kumuluzee/pull/123). You need to include all dependencies
which contain GraphQL resources (annotated with `@GraphQLApi`) as well as dependencies containing models returned from
GraphQL resources.
If all your models and resources are in the main artifact you don't need to include anything. For example to include
_my-models_ artifact use the following configuration:

```yaml
kumuluzee:
  dev:
    scan-libraries:
      - my-models
```

If you are not sure if your configuration is correct you can try disabling scanning optimization. This will scan all
dependencies but will drastically increase application startup time. Having this optimization disabled in production is
not recommended. Disable optimized scanning by using the following configuration:

```yaml
kumuluzee:
  graphql:
    scanning:
      optimize: false
```

You can also enable scan debugging by setting the following key to `true`: `kumuluzee.graphql.scanning.debug`. This
will output a verbose log of scanning configuration and progress.

## Adding Graph*i*QL (a GraphQL UI)

Graph*i*QL is a querying tool for GraphQL application.
It is the Postman equivalent for GraphQL.
You write your query, parameters and Graph*i*QL will send the request.
It also checks your query syntax and allows you to explore your schema graphically.
More information can be found [here](https://github.com/graphql/graphiql).

If you want to include GraphiQL to your project, include the following dependency:

```xml
<dependency>
    <groupId>com.kumuluz.ee.graphql</groupId>
    <artifactId>kumuluzee-graphql-ui</artifactId>
    <version>${kumuluzee-graphql.version}</version>
</dependency>
```

By default, Graph*i*QL will be accessible on `/graphiql` endpoint. You can configure the mapping or disable Graph*i*QL
with KumuluzEE Configuration framework. Example configuration:

```yaml
kumuluzee:
  graphql:
    ui:
      mapping: /api-ui
      enabled: false
```

## Using Apollo Federation

Apollo Federation is a scalable architecture for building a unified GraphQL API across multiple services, enabling teams
to develop, deploy, and manage parts of the GraphQL schema independently without requiring a monolithic schema. This
approach facilitates the development of a distributed graph that efficiently combines multiple subgraphs into a single
GraphQL endpoint.

In the context of KumuluzEE, Apollo Federation is activated by default to streamline the development of distributed
GraphQL APIs, reflecting the platform's support for modern, microservices-based architectures. For cases where
Federation is not needed or desired, you can simply disable it using the following configuration:

```yaml
kumuluzee:
  graphql:
    federation:
      enabled: false
```

| `kumuluzee-graphql` configuration               | Corresponding `smallrye-graphql` configuration       | Description                                                              | Default Value |
|:------------------------------------------------|:-----------------------------------------------------|:-------------------------------------------------------------------------|:--------------|
| `federation.enabled`                            | `smallrye.graphql.federation.enabled`                | Enable or disable Apollo Federation                                      | `true`        |
| `federation.enabled-federation-batch-resolving` | `smallrye.graphql.federation.batchResolving.enabled` | Enable batch resolving in Apollo Federation for performance optimization | `false`       |

For more detailed information on configuration refer to
the [SmallRye GraphQL documentation](https://smallrye.io/smallrye-graphql/2.7.0/federation/).

For an example of Federation integration check out the following samples:

- [kumuluzee-graphql-federation](https://github.com/kumuluz/kumuluzee-samples/tree/master/kumuluzee-graphql-federation)
- [apollo-federation-subgraph-compatibility](https://github.com/apollographql/apollo-federation-subgraph-compatibility/tree/main/implementations/kumuluzee-graphql)

### Connecting multiple Federated subgraphs

To design a unified GraphQL API from multiple federated subgraphs, you can use solutions like **Apollo Router** or
**Apollo Gateway**. These tools help in orchestrating multiple GraphQL services into a single data graph, enabling
seamless query operations across different domains or services.

- **Apollo Router** is a high-performance graph router optimized for running in production environments. It efficiently
  routes queries to the appropriate subgraphs, enabling smooth operation of your federated graph.
- **Apollo Gateway** serves as an intermediary that merges various GraphQL schemas from your federated services into a
  unified schema. It abstracts the complexity of querying multiple services, making it easier for clients to consume
  your APIs.

For detailed guidance on setting up and choosing the right solution for your architecture, refer to the official [Apollo
documentation](https://www.apollographql.com/docs/federation/building-supergraphs/router#choosing-a-router-library).

## Using kumuluzee-security on GraphQL queries

You can use [kumuluzee-security](https://github.com/kumuluz/kumuluzee-security) extension to secure GraphQL queries and
mutations with familiar annotations `@RolesAllowed`, `@PermitAll`, etc. In order to start using kumuluzee-security first
create a class that extends `GraphQLApplication` class and annotate it with `@GraphQLApplication` and `@DeclareRoles`.
For example:

```java
@GraphQLApplicationClass
@DeclareRoles({"user", "admin"})
public class CustomerApp extends GraphQLApplication {
}
```

Then secure a class annotated with `@GraphQLApi` by adding `@Secure` annotation. You can then proceed to use the
standard `@DenyAll`, `@PermitAll` and `@RolesAllowed` annotations. For example:

```java
@RequestScoped
@GraphQLApi
@Secure
public class CustomerResource {

    @Inject
    private CustomerService customerBean;

    @Query
    @PermitAll
    public List<Customer> getAllCustomers() {
        return customerBean.getCustomers();
    }

    @Query
    @RolesAllowed({"user", "admin"})
    public Customer getCustomer(@Name("customerId") String customerId) {
        return customerBean.getCustomer(customerId);
    }
}
```

For a more detailed example of kumuluzee-security integration check out the
[kumuluzee-graphql-jpa-security](https://github.com/kumuluz/kumuluzee-samples/tree/master/kumuluzee-graphql-jpa-security)
sample.

## Integration with kumuluzee-metrics

You can enable automatic metrics integration by setting the following configuration key (note that
`kumuluzee-metrics-core` dependency must be present):

```yaml
kumuluzee:
  graphql:
    metrics:
      enabled: true
```

This will add a counter and a timer to every query and mutation in the application. For a more fine-grained control
over metrics you can always use metrics annotations on your query/mutation methods. For example:

```java
@Query
@Counted(name = "get_customer_counter")
public Customer getCustomer(@Name("customerId") String customerId) {
    return customerBean.getCustomer(customerId);
}
```

## Integration with kumuluzee-bean-validation

You can validate arguments to queries and mutations by enabling Bean Validation integration with the following
configuration key (note that `kumuluzee-bean-validation-hibernate-validator` dependency must be present):

```yaml
kumuluzee:
  graphql:
    bean-validation:
      enabled: true
```

Arguments in query and mutation methods will then be verified by bean validation implementation. For example:

```java
@Query
public Customer getCustomer(@Name("customerId") @Pattern(regexp = "\\d+") String customerId) {
    return customerBean.getCustomer(customerId);
}
```

Another example:

```java
@Mutation
public Customer addNewCustomer(@Name("customer") Customer customer) {
    customerBean.saveCustomer(customer);
    return customer;
}

// Customer.java:
public class Customer {

    // ...

    @Size(min = 3, max = 15)
    private String firstName;

    // ...
}
```

## Integration with kumuluzee-rest

You can use the standard [kumuluzee-rest](https://github.com/kumuluz/kumuluzee-rest) parameters (pagination/sort/filter)
in GraphQL queries by using the `GraphQLUtils.queryParametersBuilder()` to construct `QueryParameters`
which can then be used by _kumuluzee-rest_.

For example:

```java
@Query
public StudentConnection getStudentsConnection(Long limit, Long offset, String sort, String filter) {

    QueryParameters qp = GraphQLUtils.queryParametersBuilder()
            .withQueryStringDefaults(qsd)
            .withLimit(limit)
            .withOffset(offset)
            .withOrder(sort)
            .withFilter(filter)
            .build();

    return new StudentConnection(JPAUtils.queryEntities(em, Student.class, qp),
        JPAUtils.queryEntitiesCount(em, Student.class, qp));
}
```

Query:

```graphql
query StudentsStartingWithJ {
  studentsConnection(
    offset: "0"
    limit: "10"
    sort: "studentNumber"
    filter: "name:LIKE:J%"
  ) {
    totalCount
    edges {
      studentNumber
      name
      surname
    }
  }
}
```

## Changelog

Recent changes can be viewed on Github on the [Releases Page](https://github.com/kumuluz/kumuluzee-graphql/releases).

> **For 1.0.x users, see the following
README: [kumuluzee-graphql](https://github.com/kumuluz/kumuluzee-graphql/tree/master/core)**

## Contribute

See the [contributing docs](https://github.com/kumuluz/kumuluzee-graphql/blob/master/CONTRIBUTING.md).

When submitting an issue, please follow the
[guidelines](https://github.com/kumuluz/kumuluzee-graphql/blob/master/CONTRIBUTING.md#bugs).

When submitting a bugfix, write a test that exposes the bug and fails before applying your fix. Submit the test
alongside the fix.

When submitting a new feature, add tests that cover the feature.

## License

MIT
