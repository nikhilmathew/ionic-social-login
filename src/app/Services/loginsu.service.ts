import { Injectable } from '@angular/core';
import { HttpModule, Http, Response } from '@angular/http';
import 'rxjs';
@Injectable()
export class LoginsuService {
loggedin:any;
  serverip = "192.168.0.11:3000" //--prod chat.sportsunity.co

    constructor(private http: Http){
    }
    loginSU(body) {
        var fd = new FormData();
        for (var prop in body){
            fd.append(prop,body[prop])
        }
        console.log('log in initiated',body)
        var data: any
        return this.http.post(`http://${this.serverip}/v1/login/`,fd)
            .map((response: Response) => {
                console.log(response)
                let data = response.json();
                return data
            })
    }



}
