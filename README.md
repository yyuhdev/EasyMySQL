# EasyMySQL

**EasyMySQL** is a lightweight MySQL wrapper for **Bukkit**, **Spigot**, and **Paper** plugins.
It implements the **DAO (Data Access Object) pattern** to separate database logic from plugin logic, resulting in cleaner and more maintainable code.

## Table of Contents

* [Features](#features)
* [Prerequisites](#prerequisites)
* [Example DAO Implementation](#example-dao-implementation)
* [Usage](#usage)
* [Support & Contributing](#support--contributing)
* [License](#license)

## Features

* DAO-based abstraction layer for MySQL.
* Clear separation of SQL and plugin logic.
* Built for Bukkit-style Minecraft plugins.

## Prerequisites

* Java 17+
* Bukkit, Spigot, or Paper server
* MySQL or MariaDB instance
* Build tool (Gradle or Maven)

## Example DAO Implementation

### Database Class

```java
public class ExampleDatabase implements DatabaseProvider<Example> {

    private final HikariDataSource source;

    public ExampleDatabase(final HikariDataSource source) {
        this.source = source;
    }

    @Override
    public void start() {
        try (final Connection connection = source.getConnection()) {
            try (final PreparedStatement stmt = connection.prepareStatement("""
                    CREATE TABLE IF NOT EXISTS example (
                    uuid TEXT PRIMARY KEY,
                    amount INTEGER NOT NULL
                    );
                    """)) {
                stmt.execute();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void save(Example example) {
        try (final Connection connection = source.getConnection()) {
            try (final PreparedStatement stmt = connection.prepareStatement("""
                    INSERT INTO example (uuid, amount)
                    VALUES (?,?)
                    ON DUPLICATE KEY UPDATE amount = VALUES(amount);
                    """)) {
                stmt.setString(1, example.uuid().toString());
                stmt.setInt(2, example.amount());
                stmt.execute();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<Example> get(final UUID uuid) {
        try (final Connection connection = source.getConnection()) {
            try (final PreparedStatement prts = connection.prepareStatement("""
                    SELECT amount
                    FROM example
                    WHERE uuid = ?
                    """)) {
                prts.setString(1, uuid.toString());
                final ResultSet set = prts.executeQuery();

                if (set.next()) {
                    final int amount = set.getInt("amount");
                    return Optional.of(new Example(uuid, amount));
                }

                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
```

### Invocation

```java
@Override
public void onEnable() {
    DatabaseManager.getInstance();
}

private void register() {
    providers.put(Example.class, new ExampleDatabase(dataSource));

    for (final DatabaseProvider<?> provider : providers.values()) {
        provider.start();
    }
}
```

## Usage

**Saving**

```java
DatabaseManager.getInstance().save(Example.class, new Example(UUID.randomUUID(), 1));
```

**Retrieving**

```java
DatabaseManager.getInstance().get(Example.class, uuid)
        .thenAccept(example -> {
            // your code
        });
```

## Support & Contributing

Contributions and improvements are welcome.
Please use the GitHub repository to report issues or submit pull requests.

## License

This project is licensed under the [MIT License](LICENSE).
