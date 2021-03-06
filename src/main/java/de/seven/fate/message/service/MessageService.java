package de.seven.fate.message.service;

import de.seven.fate.message.dao.MessageDAO;
import de.seven.fate.message.domain.Message;
import de.seven.fate.message.enums.MessageType;
import de.seven.fate.person.dao.PersonDAO;
import de.seven.fate.person.domain.Person;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.lang3.Validate.notNull;

@Slf4j
@Service
public class MessageService {

    @Inject
    private MessageDAO dao;

    @Inject
    private PersonDAO personDAO;


    public Message getMessage(Message message) {

        return dao.findOne(message.getId());
    }

    public List<Message> findMessagesByPerson(String ldapId) {

        return dao.findByPersonLdapId(ldapId);
    }

    public List<Message> findMessagesByPersonAndType(String userName, MessageType messageType) {

        return dao.findByPersonLdapIdAndMessageType(userName, messageType);
    }

    public void removeMessage(Message message) {

        message.setPerson(null);
        dao.delete(message);
    }

    public void removeMessage(Long messageId) {

        removeMessage(dao.findOne(messageId));
    }

    public void saveMessage(Message message) {

        saveMessage(Arrays.asList(message), message.getPerson());
    }


    public void saveMessage(List<Message> messages) {

        messages.forEach(this::saveMessage);
    }

    public Message updateMessage(Message message) {
        return dao.save(message);
    }

    /**
     * save message only if person exist in DB
     */
    public void saveMessage(List<Message> messages, Person person) {
        notNull(messages);
        notNull(person);

        if (messages.isEmpty()) {
            log.warn("ignore empty messages by: " + person.getLdapId());

            return;
        }

        Person attachedPerson = personDAO.findByLdapId(person.getLdapId());

        if (attachedPerson == null) {

            log.warn("unable to find person by: " + person.getLdapId() + " message will be ignored");

            attachedPerson = personDAO.save(person);
            // return; //NOSONAR this is workaround and will be removed in production env.
        }

        for (Message message : messages) {
            message.setPerson(attachedPerson);
        }

        dao.save(messages);

        log.debug("save [" + messages.size() + "] message(s) by person: " + person.getLdapId());

    }

    public void removeAllMessage(String personLdapId) {
        notNull(personLdapId);

        Person person = personDAO.findByLdapId(personLdapId);

        dao.delete(person.getMessages());
    }


    public void markMessage(List<Long> messageIds, MessageType messageType) {
        notNull(messageType);

        if (isEmpty(messageIds)) {
            return;
        }

        int executeUpdate = dao.markMessage(messageIds, messageType);

        log.info("update " + executeUpdate + " messages to type: " + messageType);
    }
}
