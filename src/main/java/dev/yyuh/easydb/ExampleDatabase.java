package dev.yyuh.easydb;

import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

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
                    """
            )) {
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
