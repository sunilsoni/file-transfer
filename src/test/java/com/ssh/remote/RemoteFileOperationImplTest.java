package com.ssh.remote;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.sshd.SshServer;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.Session;
import org.apache.sshd.common.file.FileSystemView;
import org.apache.sshd.common.file.nativefs.NativeFileSystemFactory;
import org.apache.sshd.common.file.nativefs.NativeFileSystemView;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.command.ScpCommandFactory;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.server.sftp.SftpSubsystem;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.vngx.jsch.ChannelSftp;
import org.vngx.jsch.ChannelType;
import org.vngx.jsch.JSch;
import org.vngx.jsch.config.SessionConfig;
import org.vngx.jsch.exception.JSchException;
import org.vngx.jsch.exception.SftpException;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
public class RemoteFileOperationImplTest {

    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final String SFTPDATA = "sftpdata";
    private static final String JOB_CSV = "events.csv";
    private static final int PORT = 1234;
    @ClassRule
    public static TemporaryFolder testFolder = new TemporaryFolder(new File("D:\\Work\\ssh\\temp"));//new TemporaryFolder();
    private static SshServer sshd;
    @Autowired
    RemoteFileOperationImpl remoteFileOperationImpl;

    @BeforeClass
    public static void setupSSHServer() throws IOException {
        sshd = SshServer.setUpDefaultServer();
        sshd.setFileSystemFactory(new NativeFileSystemFactory() {
            @Override
            public FileSystemView createFileSystemView(final Session session) {
                return new NativeFileSystemView(session.getUsername(), false) {
                    @Override
                    public String getVirtualUserDir() {
                        return testFolder.getRoot().getAbsolutePath();
                    }
                };
            }

            ;
        });
        sshd.setPort(PORT);
        sshd.setSubsystemFactories(Arrays.<NamedFactory<Command>>asList(new SftpSubsystem.Factory()));
        sshd.setCommandFactory(new ScpCommandFactory());
        sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(testFolder.newFile("hostkey.ser").getAbsolutePath()));
        sshd.setPasswordAuthenticator(new PasswordAuthenticator() {
            @Override
            public boolean authenticate(final String username, final String password, final ServerSession session) {
                return StringUtils.equals(username, USERNAME) && StringUtils.equals(password, PASSWORD);
            }
        });
        sshd.start();
    }


    @AfterClass
    public static void tearDown() throws Exception {
        try {
            sshd.stop(true);
        } catch (Exception e) {
            // do nothing
        }
    }

    @Test
    public void testUploadingFiles() throws Exception {
        sendFile(JOB_CSV, SFTPDATA);
        assertThat(new File(testFolder.getRoot(), "test.txt").exists(), equalTo(true));
    }

    private void sendFile(final String filename, final String contents) throws Exception {
        SessionConfig config = new SessionConfig();
        config.setProperty(SessionConfig.STRICT_HOST_KEY_CHECKING, "no");
        org.vngx.jsch.Session session = JSch.getInstance().createSession(USERNAME, "localhost", PORT, config);

        session.connect(PASSWORD.getBytes(StandardCharsets.UTF_8));
        ChannelSftp sftpChannel = session.openChannel(ChannelType.SFTP);
        sftpChannel.connect();

        OutputStream out = sftpChannel.put(filename);
        out.write(contents.getBytes(StandardCharsets.UTF_8));
        IOUtils.closeQuietly(out);
        sftpChannel.disconnect();
        session.disconnect();
    }

    @Test
    public void mkdir() throws SftpException, JSchException {
        log.info("Starting mkdir");

        //remoteFileOperationImpl.notExists("/test");
        remoteFileOperationImpl.mkdir("/test");

    }

    @Test
    public void cd() throws SftpException {
        log.info("Starting cd");
        remoteFileOperationImpl.cd("test");
    }

    @Test
    public void rm() throws SftpException {
        log.info("Starting rm");
        remoteFileOperationImpl.rm("test.csv");
    }

    @Test
    public void rmdir() throws SftpException {
        log.info("Starting rmdir");
        remoteFileOperationImpl.rmdir("test");
    }

    @Test
    public void notExists() throws SftpException, JSchException {
        log.info("Starting notExists");
        remoteFileOperationImpl.notExists("/test");
    }
}