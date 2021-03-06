package persistence.dao.jdbcTemplateImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import persistence.dao.abstractDAOImpl.AbstractImageDAO;
import persistence.dao.interfaces.ImageDAO;
import persistence.exception.PersistenceException;
import persistence.exception.RecordNotFoundException;
import persistence.struct.Image;
import persistence.utils.DatabaseUtils;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class ImageDAOImpl extends AbstractImageDAO implements ImageDAO {

    private NamedParameterJdbcOperations jdbcTemplate;

    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    // TABLE
    static final String TABLE_NAME = "image";

    // FIELDS IN DATABASE
    static final String ID = "id";
    static final String USER_ID = "userId";
    static final String NAME = "name";
    static final String COMMENT = "comment";
    static final String TIMESTAMP = "timestamp";

    // EXTRA BIND COLUMNS
    static final String ORDER = "order";
    static final String LIMIT = "limit";
    static final String OFFSET = "offset";

    // SQL
    private static final String INSERT = "INSERT INTO " + TABLE_NAME + "("
            + USER_ID + ","
            + NAME + ","
            + COMMENT + ") VALUES (:" + USER_ID + ",:" + NAME + ",:" + COMMENT + ")";

    private static final String UPDATE = "UPDATE " + TABLE_NAME + " SET "
            + NAME + "=:" + NAME + ", "
            + COMMENT + "=:" + COMMENT + ","
            + TIMESTAMP + "=CURRENT_TIME() "
            + " WHERE " + ID + "=:" + ID;

    private static final String DELETE = "DELETE FROM " + TABLE_NAME
            + " WHERE " + ID + "=:" + ID;

    private static final String FETCH = "SELECT * FROM " + TABLE_NAME;

    private static final String BY_PRIMARY = "SELECT * FROM " + TABLE_NAME
            + " WHERE " + ID + "=:" + ID;

    private static final String COUNT = "SELECT COUNT(*) FROM " + TABLE_NAME;

    private static final String FETCH_WITH_OFFSET = "SELECT * FROM " + TABLE_NAME
            + " ORDER BY :" + ORDER
            + " LIMIT :" + LIMIT + " OFFSET :" + OFFSET;

    private NamedParameterJdbcOperations getTemplate() {
        return jdbcTemplate;
    }

    private static final RowMapper<Image> IMAGE_ROW_MAPPER = new RowMapper<Image>() {

        @Override
        public Image mapRow(ResultSet rs, int rowNum) throws SQLException {
            Image image = new Image();

            image.setId(rs.getInt(ID));
            image.setUserId(rs.getInt(USER_ID));
            image.setName(rs.getString(NAME));
            image.setComment(rs.getString(COMMENT));
            image.setTimestamp(rs.getTimestamp(TIMESTAMP));

            return image;
        }
    };

    @PostConstruct
    protected void initScheme() throws PersistenceException {
        String createTable = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + "("
                + ID + " INTEGER PRIMARY KEY AUTO_INCREMENT,"
                + USER_ID + " INTEGER NOT NULL,"
                + NAME + " VARCHAR(" + super.getNameLimit() + ") NOT NULL ,"
                + COMMENT + " VARCHAR(" + super.getCommentLimit() + ") NOT NULL DEFAULT '',"
                + TIMESTAMP + " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                " FOREIGN KEY(" + USER_ID + ") REFERENCES " + UserDAOImpl.TABLE_NAME + "(" + UserDAOImpl.ID + "))";

        getTemplate().update(createTable, Collections.EMPTY_MAP);
    }

    @Override
    protected void insertImpl(Image image) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        Map<String, Object> params = new HashMap<>(3, 1F);
        params.put(USER_ID, image.getUserId());
        params.put(NAME, image.getName());
        params.put(COMMENT, image.getComment());

        SqlParameterSource parameterSource = new MapSqlParameterSource(params);

        int insertCount = getTemplate().update(INSERT, parameterSource, keyHolder);

        if (insertCount == 0) {
            throw new PersistenceException("Creating image failed, no rows affected after insert. SQL " + INSERT);
        }

        Number key = keyHolder.getKey();

        if (key == null) {
            throw new PersistenceException("Creating image failed, no generated key obtained.");
        }

        image.setId(key.intValue());
    }

    @Override
    protected int updateImpl(Image image) throws RecordNotFoundException {
        Map<String, Object> params = new HashMap<>(3, 1F);
        params.put(ID, image.getId());
        params.put(NAME, image.getName());
        params.put(COMMENT, image.getComment());

        return getTemplate().update(UPDATE, params);
    }

    @Override
    protected int deleteImpl(Integer id) throws RecordNotFoundException {
        Map<String, Object> params = new HashMap<>(1, 1F);
        params.put(ID, id);

        return getTemplate().update(DELETE, params);
    }

    @Override
    public List<Image> fetch() {
        return getTemplate().query(FETCH, Collections.EMPTY_MAP, IMAGE_ROW_MAPPER);
    }

    @Override
    protected Image fetchByPrimaryImpl(Integer id) {
        Map<String, Object> params = new HashMap<>(1, 1F);
        params.put(ID, id);

        return DatabaseUtils.getSingleRowOrNull(getTemplate(), BY_PRIMARY, params, IMAGE_ROW_MAPPER);
    }

    @Override
    public int getCount() {
        return getTemplate().queryForInt(COUNT, Collections.EMPTY_MAP);
    }

    @Override
    protected List<Image> fetchWithOffsetImpl(int offset, int limit) {
        Map<String, Object> params = new HashMap<>(1, 1F);
        params.put(ORDER, getOrderColumn());
        params.put(LIMIT, limit);
        params.put(OFFSET, offset);

        return getTemplate().query(FETCH_WITH_OFFSET, params, IMAGE_ROW_MAPPER);
    }
}