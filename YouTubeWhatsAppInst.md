# Reason not to work on ordinary device behind proxy #

Some apps do not respect android OS proxy settings.
To make them work http flow must be redirected.
For this SandroProxy needs to create iptable rules. So the device must be rooted.


# HowTo #

1. On APP tab check all three checkboxes

![http://2.bp.blogspot.com/-Nc8c_H8IPzU/UzaT15YqWCI/AAAAAAAAAYM/pSOawMYFfXc/s1600/sandroproxy_apps_tab.png](http://2.bp.blogspot.com/-Nc8c_H8IPzU/UzaT15YqWCI/AAAAAAAAAYM/pSOawMYFfXc/s1600/sandroproxy_apps_tab.png)

2. In Settings enable transparent proxy

![http://2.bp.blogspot.com/-sdRv7zOUpOw/UzaT0Nfz1SI/AAAAAAAAAYE/FVZl2sf_a-U/s1600/transparent_proxy_on.png](http://2.bp.blogspot.com/-sdRv7zOUpOw/UzaT0Nfz1SI/AAAAAAAAAYE/FVZl2sf_a-U/s1600/transparent_proxy_on.png)

3. Set your proxy settings

![http://3.bp.blogspot.com/--WfeN4pAvro/UzaT-JrYMxI/AAAAAAAAAYU/TuDCTCNM_Hs/s320/sandroproxy_connect_to_other_proxy.png](http://3.bp.blogspot.com/--WfeN4pAvro/UzaT-JrYMxI/AAAAAAAAAYU/TuDCTCNM_Hs/s320/sandroproxy_connect_to_other_proxy.png)


4. Restart SandroProxy