import { NavController } from 'ionic-angular';
import { Angular2SocialLoginService } from './../../Services/login.service';
import { AuthService } from './../../Services/auth.service';
import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-initial',
  templateUrl: './initial.component.html',
  styleUrls: ['./initial.component.scss']
})
export class InitialComponent implements OnInit {

  sub: any;
  // user: any;
  constructor(public _auth: AuthService, private login: Angular2SocialLoginService,public navCtrl: NavController) {

  }

  signIn(provider) {
    console.log("login")
    this.sub = this._auth.login(provider).subscribe(
      (data) => {
        console.log(data);
        this.login.loginData = data;
       // this.router.navigate(["/signed"],{ skipLocationChange: true })
        // this.user = data;
      }
    )
  }
  ngOnInit() {
  }

}
