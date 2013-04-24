#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <stddef.h>
#include <sys/socket.h>
#include <sys/un.h>

/*
 * Create a UNIX-domain socket address in the Linux "abstract namespace".
 *
 * The socket code doesn't require null termination on the filename, but
 * we do it anyway so string functions work.
 */
int makeAddr(const char* name, struct sockaddr_un* pAddr, socklen_t* pSockLen)
{
    int nameLen = strlen(name);
    if (nameLen >= (int) sizeof(pAddr->sun_path) -1)  /* too long? */
        return -1;
    pAddr->sun_path[0] = '\0';  /* abstract namespace */
    strcpy(pAddr->sun_path+1, name);
    pAddr->sun_family = AF_LOCAL;
    *pSockLen = 1 + nameLen + offsetof(struct sockaddr_un, sun_path);
    return 0;
}

int main(int argc, char** argv)
{
    static const char* message = "hello, world!";
    struct sockaddr_un sockAddr;
    socklen_t sockLen;
    int result = 1;

    printf("Helouuu!!!\n");

    if (argc != 3 || (argv[1][0] != 'c' && argv[1][0] != 's')) {
        printf("Usage: {c|s} unix_socket_name\n");
        return 2;
    }

    if (makeAddr(argv[2], &sockAddr, &sockLen) < 0)
        return 1;
    int fd = socket(AF_LOCAL, SOCK_STREAM, PF_UNIX);
    if (fd < 0) {
        perror("client socket()");
        return 1;
    }

    if (argv[1][0] == 'c') {
        printf("CLIENT %s\n", sockAddr.sun_path+1);

        if (connect(fd, (const struct sockaddr*) &sockAddr, sockLen) < 0) {
            perror("client connect()");
            goto bail;
        }
        if (write(fd, message, strlen(message)+1) < 0) {
            perror("client write()");
            goto bail;
        }
    } else if (argv[1][0] == 's') {
        printf("SERVER %s\n", sockAddr.sun_path+1);
        if (bind(fd, (const struct sockaddr*) &sockAddr, sockLen) < 0) {
            perror("server bind()");
            goto bail;
        }
        if (listen(fd, 5) < 0) {
            perror("server listen()");
            goto bail;
        }
        int clientSock = accept(fd, NULL, NULL);
        if (clientSock < 0) {
            perror("server accept");
            goto bail;
        }
        char buf[64];
        int count = read(clientSock, buf, sizeof(buf));
        close(clientSock);
        if (count < 0) {
            perror("server read");
            goto bail;
        }
        printf("GOT: '%s'\n", buf);
    }
    result = 0;

bail:
    close(fd);
    return result;
}
