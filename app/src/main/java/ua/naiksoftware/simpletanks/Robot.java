package ua.naiksoftware.simpletanks;

/**
 * Programmable bot for game
 *
 * Created by Naik on 03.08.15.
 */
public class Robot extends User {

    /**
     * Создать нового бота (на клиенте или сервере)
     *
     * @param name отображаемое имя
     * @param id   если передано {@code GEN_NEW_ID} то будет сгенерирован новый ID
     * @param ip   IP юзера, или метка что это Вы или владелец сервера (не имеет особого смысла у робота)
     */
    public Robot(String name, User owner, long id) {
        super(name + " [bot]", id, owner.getIp());
    }
}
