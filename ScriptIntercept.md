There are two script that are triggered on every http request.
One when request came in (Request tag) and when response (Response tag) from server is retrieved.

Scripts can trigger gui or just change request/response object.
If result variable is set to true gui will show.
In request script there is also intercept\_response flag.
If set to true response gui will trigger also.

Example of request script that do not show gui, just add request header:

![http://1.bp.blogspot.com/-k-AgrsGen1g/UzpceRsSwKI/AAAAAAAAAYo/p6vH8Bo5imQ/s1600/intercept_request.png](http://1.bp.blogspot.com/-k-AgrsGen1g/UzpceRsSwKI/AAAAAAAAAYo/p6vH8Bo5imQ/s1600/intercept_request.png)

http://code.google.com/p/sandrop/source/browse/projects/SandroProxyPlugin/intercept_request.txt


Example of response script that change content on wiki pages:

![http://3.bp.blogspot.com/-yt4x6coZZKo/Uzpce_dSGcI/AAAAAAAAAY0/TzJZf0eIqJI/s1600/intercept_response.png](http://3.bp.blogspot.com/-yt4x6coZZKo/Uzpce_dSGcI/AAAAAAAAAY0/TzJZf0eIqJI/s1600/intercept_response.png)

http://code.google.com/p/sandrop/source/browse/projects/SandroProxyPlugin/intercept_response.txt

SandroProxy must be in capture mode:

![http://3.bp.blogspot.com/-VAHSe0TvxJk/UzpceR1PYuI/AAAAAAAAAYs/4AvxUS9YBlg/s1600/capture_data_flag.png](http://3.bp.blogspot.com/-VAHSe0TvxJk/UzpceR1PYuI/AAAAAAAAAYs/4AvxUS9YBlg/s1600/capture_data_flag.png)

To capture https also SandroProxy CA must be export to android CA store:

![http://1.bp.blogspot.com/-ITJKcv6wijI/UzpceSJ4PII/AAAAAAAAAYk/AbHS7LZBIy4/s1600/export_ca_to_store.png](http://1.bp.blogspot.com/-ITJKcv6wijI/UzpceSJ4PII/AAAAAAAAAYk/AbHS7LZBIy4/s1600/export_ca_to_store.png)