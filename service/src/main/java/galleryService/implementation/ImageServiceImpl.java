package galleryService.implementation;

import galleryService.exception.EmptyImageException;
import galleryService.interfaces.ImageService;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import persistence.dao.interfaces.FileManager;
import persistence.dao.interfaces.ImageDAO;
import persistence.exception.ValidationException;
import persistence.struct.Image;
import persistence.struct.User;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: agnidash
 * Date: 4/24/13
 * Time: 6:03 PM
 */
@Service
public class ImageServiceImpl implements ImageService {

    private ImageDAO imageDAO;
    private FileManager fileManager;

    @Required
    public void setImageDAO(ImageDAO imageDAO) {
        this.imageDAO = imageDAO;
    }

    @Required
    public void setFileManager(FileManager fileManager) {
        this.fileManager = fileManager;
    }

    @Transactional
    public void addImage(User user, String name, String comment, byte[] data) throws ValidationException, IOException {

        if (data == null || data.length == 0) {
            throw new EmptyImageException("param data must not be null");
        }

        Image image = new Image();
        image.setName(name);
        image.setComment(comment);
        image.setUserId(user.getId());

        ImageDAO dao = getImageDAO();
        dao.insert(image);

        int id = image.getId();
        try {
            getFileManager().createFile(data, id);
        } catch (IOException e) {
            dao.delete(id);

            throw e;
        }
    }

    private FileManager getFileManager() {
        return fileManager;
    }

    private ImageDAO getImageDAO() {
        return imageDAO;
    }
}
